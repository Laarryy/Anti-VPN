package me.egg82.avpn;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import org.bukkit.ChatColor;

import com.google.common.collect.ImmutableSet;

import me.egg82.avpn.core.RedisSubscriber;
import me.egg82.avpn.sql.mysql.FetchQueueMySQLCommand;
import me.egg82.avpn.sql.sqlite.ClearDataSQLiteCommand;
import me.egg82.avpn.utils.RedisUtil;
import ninja.egg82.bukkit.BasePlugin;
import ninja.egg82.bukkit.processors.CommandProcessor;
import ninja.egg82.bukkit.processors.EventProcessor;
import ninja.egg82.bukkit.services.ConfigRegistry;
import ninja.egg82.bukkit.utils.YamlUtil;
import ninja.egg82.enums.BaseSQLType;
import ninja.egg82.events.CompleteEventArgs;
import ninja.egg82.exceptionHandlers.GameAnalyticsExceptionHandler;
import ninja.egg82.exceptionHandlers.IExceptionHandler;
import ninja.egg82.exceptionHandlers.RollbarExceptionHandler;
import ninja.egg82.exceptionHandlers.builders.GameAnalyticsBuilder;
import ninja.egg82.exceptionHandlers.builders.RollbarBuilder;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.messaging.IMessageHandler;
import ninja.egg82.plugin.utils.PluginReflectUtil;
import ninja.egg82.sql.ISQL;
import ninja.egg82.utils.FileUtil;
import ninja.egg82.utils.ThreadUtil;
import ninja.egg82.utils.TimeUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class AntiVPN extends BasePlugin {
	//vars
	private static VPNAPI api = new VPNAPI();
	
	private int numMessages = 0;
	private int numCommands = 0;
	private int numEvents = 0;
	private int numTicks = 0;
	
	private IExceptionHandler exceptionHandler = null;
	private String version = getDescription().getVersion();
	
	//constructor
	public AntiVPN() {
		super();
		
		getLogger().setLevel(Level.WARNING);
		IExceptionHandler oldExceptionHandler = ServiceLocator.getService(IExceptionHandler.class);
		ServiceLocator.removeServices(IExceptionHandler.class);
		
		ServiceLocator.provideService(RollbarExceptionHandler.class, false);
		exceptionHandler = ServiceLocator.getService(IExceptionHandler.class);
		oldExceptionHandler.disconnect();
		exceptionHandler.connect(new RollbarBuilder("dccf7919d6204dfea740702ad41ee08c", "production", version, getServerId()), "AntiVPN");
		exceptionHandler.setUnsentExceptions(oldExceptionHandler.getUnsentExceptions());
		exceptionHandler.setUnsentLogs(oldExceptionHandler.getUnsentLogs());
	}
	
	//public
	public static VPNAPI getAPI() {
		return api;
	}
	
	public void onLoad() {
		super.onLoad();
		
		PluginReflectUtil.addServicesFromPackage("me.egg82.avpn.registries", true);
		PluginReflectUtil.addServicesFromPackage("me.egg82.avpn.lists", true);
		
		ServiceLocator.getService(ConfigRegistry.class).load(YamlUtil.getOrLoadDefaults(getDataFolder().getAbsolutePath() + FileUtil.DIRECTORY_SEPARATOR_CHAR + "config.yml", "config.yml", true));
		Config.debug = ServiceLocator.getService(ConfigRegistry.class).getRegister("debug", Boolean.class).booleanValue();
		if (Config.debug) {
			printInfo("Debug enabled");
		}
		String[] sources = ServiceLocator.getService(ConfigRegistry.class).getRegister("sources.order", String.class).split(",\\s?");
		Set<String> sourcesOrdered = new LinkedHashSet<String>();
		for (String source : sources) {
			if (Config.debug) {
				printInfo("Adding potential source " + source + ".");
			}
			sourcesOrdered.add(source);
		}
		for (Iterator<String> i = sourcesOrdered.iterator(); i.hasNext();) {
			String source = i.next();
			if (!ServiceLocator.getService(ConfigRegistry.class).hasRegister("sources." + source + ".enabled")) {
				if (Config.debug) {
					printInfo(source + " does not have an \"enabled\" flag. Assuming disabled. Removing.");
				}
				i.remove();
				continue;
			}
			if (!ServiceLocator.getService(ConfigRegistry.class).getRegister("sources." + source + ".enabled", Boolean.class).booleanValue()) {
				if (Config.debug) {
					printInfo(source + " is disabled. Removing.");
				}
				i.remove();
				continue;
			}
		}
		Config.sources = ImmutableSet.copyOf(sourcesOrdered);
		Config.sourceCacheTime = TimeUtil.getTime(ServiceLocator.getService(ConfigRegistry.class).getRegister("sources.cacheTime", String.class));
		if (Config.debug) {
			printInfo("Source cache time: " + TimeUtil.timeToHoursMinsSecs(Config.sourceCacheTime));
		}
		Config.kickMessage = ServiceLocator.getService(ConfigRegistry.class).getRegister("kickMessage", String.class);
		Config.async = ServiceLocator.getService(ConfigRegistry.class).getRegister("async", Boolean.class).booleanValue();
		if (Config.debug) {
			printInfo((Config.async) ? "Async enabled" : "Async disabled");
		}
		Config.kick = ServiceLocator.getService(ConfigRegistry.class).getRegister("kick", Boolean.class).booleanValue();
		if (Config.debug) {
			printInfo((Config.kick) ? "Set to kick" : "Set to API-only");
		}
	}
	
	public void onEnable() {
		super.onEnable();
		
		List<IMessageHandler> services = ServiceLocator.removeServices(IMessageHandler.class);
		for (IMessageHandler handler : services) {
			try {
				handler.close();
			} catch (Exception ex) {
				
			}
		}
		
		Loaders.loadRedis();
		Loaders.loadRabbit();
		Loaders.loadStorage();
		
		numCommands = ServiceLocator.getService(CommandProcessor.class).addHandlersFromPackage("me.egg82.avpn.commands", PluginReflectUtil.getCommandMapFromPackage("me.egg82.avpn.commands", false, null, "Command"), false);
		numEvents = ServiceLocator.getService(EventProcessor.class).addHandlersFromPackage("me.egg82.avpn.events");
		if (ServiceLocator.hasService(IMessageHandler.class)) {
			numMessages = ServiceLocator.getService(IMessageHandler.class).addHandlersFromPackage("me.egg82.avpn.messages");
		}
		numTicks = PluginReflectUtil.addServicesFromPackage("me.egg82.avpn.ticks", false);
		
		ThreadUtil.submit(new Runnable() {
			public void run() {
				try (Jedis redis = RedisUtil.getRedis()) {
					if (redis != null) {
						redis.subscribe(new RedisSubscriber(), "avpn");
					}
				}
			}
		});
		
		enableMessage();
		
		ThreadUtil.rename(getName());
		ThreadUtil.schedule(checkExceptionLimitReached, 60L * 60L * 1000L);
		ThreadUtil.schedule(onFetchQueueThread, 10L * 1000L);
	}
	public void onDisable() {
		super.onDisable();
		
		ThreadUtil.shutdown(1000L);
		
		List<ISQL> sqls = ServiceLocator.removeServices(ISQL.class);
		for (ISQL sql : sqls) {
			sql.disconnect();
		}
		
		JedisPool jedisPool = ServiceLocator.getService(JedisPool.class);
		if (jedisPool != null) {
			jedisPool.close();
		}
		
		List<IMessageHandler> services = ServiceLocator.removeServices(IMessageHandler.class);
		for (IMessageHandler handler : services) {
			try {
				handler.close();
			} catch (Exception ex) {
				
			}
		}
		
		ServiceLocator.getService(CommandProcessor.class).clear();
		ServiceLocator.getService(EventProcessor.class).clear();
		
		disableMessage();
	}
	
	//private
	private Runnable onFetchQueueThread = new Runnable() {
		public void run() {
			CountDownLatch latch = new CountDownLatch(1);
			
			BiConsumer<Object, CompleteEventArgs<?>> complete = (s, e) -> {
				latch.countDown();
				ISQL sql = ServiceLocator.getService(ISQL.class);
				if (sql.getType() == BaseSQLType.MySQL) {
					FetchQueueMySQLCommand c = (FetchQueueMySQLCommand) s;
					c.onComplete().detatchAll();
				} else if (sql.getType() == BaseSQLType.SQLite) {
					ClearDataSQLiteCommand c = (ClearDataSQLiteCommand) s;
					c.onComplete().detatchAll();
				}
			};
			
			ISQL sql = ServiceLocator.getService(ISQL.class);
			if (sql.getType() == BaseSQLType.MySQL) {
				FetchQueueMySQLCommand command = new FetchQueueMySQLCommand();
				command.onComplete().attach(complete);
				command.start();
			} else if (sql.getType() == BaseSQLType.SQLite) {
				ClearDataSQLiteCommand command = new ClearDataSQLiteCommand();
				command.onComplete().attach(complete);
				command.start();
			}
			
			try {
				latch.await();
			} catch (Exception ex) {
				ServiceLocator.getService(IExceptionHandler.class).silentException(ex);
			}
			
			ThreadUtil.schedule(onFetchQueueThread, 10L * 1000L);
		}
	};
	private Runnable checkExceptionLimitReached = new Runnable() {
		public void run() {
			if (exceptionHandler.isLimitReached()) {
				IExceptionHandler oldExceptionHandler = ServiceLocator.getService(IExceptionHandler.class);
				ServiceLocator.removeServices(IExceptionHandler.class);
				
				ServiceLocator.provideService(GameAnalyticsExceptionHandler.class, false);
				exceptionHandler = ServiceLocator.getService(IExceptionHandler.class);
				oldExceptionHandler.disconnect();
				exceptionHandler.connect(new GameAnalyticsBuilder("10b55aa4f41d64ff258f9c66a5fbf9ec", "3794acfebab1122e852d73bbf505c37f42bf3f41", version, getServerId()), getName());
				exceptionHandler.setUnsentExceptions(oldExceptionHandler.getUnsentExceptions());
				exceptionHandler.setUnsentLogs(oldExceptionHandler.getUnsentLogs());
			}
			
			ThreadUtil.schedule(checkExceptionLimitReached, 60L * 60L * 1000L);
		}
	};
	
	private void enableMessage() {
		printInfo(ChatColor.AQUA + "AntiVPN enabled.");
		printInfo(ChatColor.GREEN + "[Version " + getDescription().getVersion() + "] " + ChatColor.RED + numCommands + " commands " + ChatColor.LIGHT_PURPLE + numEvents + " events " + ChatColor.YELLOW + numTicks + " tick handlers " + ChatColor.BLUE + numMessages + " message handlers");
		printInfo(ChatColor.WHITE + "[AntiVPN] " + ChatColor.GRAY + "Attempting to load compatibility with Bukkit version " + getGameVersion());
	}
	private void disableMessage() {
		printInfo(ChatColor.GREEN + "--== " + ChatColor.LIGHT_PURPLE + "AntiVPN Disabled" + ChatColor.GREEN + " ==--");
	}
}

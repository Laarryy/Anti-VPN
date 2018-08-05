package me.egg82.avpn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

import me.egg82.avpn.core.RedisSubscriber;
import me.egg82.avpn.debug.BungeeDebugPrinter;
import me.egg82.avpn.debug.IDebugPrinter;
import me.egg82.avpn.sql.mysql.FetchQueueMySQLCommand;
import me.egg82.avpn.sql.sqlite.ClearDataSQLiteCommand;
import me.egg82.avpn.utils.RedisUtil;
import net.md_5.bungee.api.ChatColor;
import ninja.egg82.analytics.exceptions.GameAnalyticsExceptionHandler;
import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.analytics.exceptions.RollbarExceptionHandler;
import ninja.egg82.bungeecord.BasePlugin;
import ninja.egg82.bungeecord.processors.CommandProcessor;
import ninja.egg82.bungeecord.processors.EventProcessor;
import ninja.egg82.enums.BaseSQLType;
import ninja.egg82.events.CompleteEventArgs;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.enums.SenderType;
import ninja.egg82.plugin.messaging.IMessageHandler;
import ninja.egg82.plugin.utils.PluginReflectUtil;
import ninja.egg82.sql.ISQL;
import ninja.egg82.utils.ThreadUtil;
import ninja.egg82.utils.TimeUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class AntiVPN extends BasePlugin {
	//vars
	private int numMessages = 0;
	private int numCommands = 0;
	private int numEvents = 0;
	
	private IExceptionHandler exceptionHandler = null;
	private String version = null;
	
	//constructor
	public AntiVPN() {
		super();
	}
	
	//public
	public static VPNAPI getAPI() {
		return VPNAPI.getInstance();
	}
	
	public void onLoad() {
		super.onLoad();
		
		version = getDescription().getVersion();
		
		exceptionHandler = ServiceLocator.getService(IExceptionHandler.class);
		getLogger().setLevel(Level.WARNING);
		
		ServiceLocator.provideService(BungeeDebugPrinter.class);
		
		PluginReflectUtil.addServicesFromPackage("me.egg82.avpn.registries", true);
		PluginReflectUtil.addServicesFromPackage("me.egg82.avpn.lists", true);
		
		Configuration config = ConfigLoader.getConfig("config.yml", "config.yml");
		
		Config.debug = config.getNode("debug").getBoolean();
		if (Config.debug) {
			ServiceLocator.getService(IDebugPrinter.class).printInfo("Debug enabled");
		}
		List<String> sources = null;
		try {
			sources = config.getNode("sources", "order").getList(TypeToken.of(String.class));
		} catch (Exception ex) {
			sources = new ArrayList<String>();
		}
		Set<String> sourcesOrdered = new LinkedHashSet<String>();
		for (String source : sources) {
			if (Config.debug) {
				ServiceLocator.getService(IDebugPrinter.class).printInfo("Adding potential source " + source + ".");
			}
			sourcesOrdered.add(source);
		}
		for (Iterator<String> i = sourcesOrdered.iterator(); i.hasNext();) {
			String source = i.next();
			if (!config.getNode("sources", source, "enabled").getBoolean()) {
				if (Config.debug) {
					ServiceLocator.getService(IDebugPrinter.class).printInfo(source + " is disabled. Removing.");
				}
				i.remove();
			}
		}
		Config.sources = ImmutableSet.copyOf(sourcesOrdered);
		try {
			Config.ignore = ImmutableSet.copyOf(config.getNode("ignore").getList(TypeToken.of(String.class)));
		} catch (Exception ex) {
			Config.ignore = ImmutableSet.of();
		}
		Config.sourceCacheTime = TimeUtil.getTime(config.getNode("sources", "cacheTime").getString("6hours"));
		if (Config.debug) {
			ServiceLocator.getService(IDebugPrinter.class).printInfo("Source cache time: " + TimeUtil.timeToHoursMinsSecs(Config.sourceCacheTime));
		}
		Config.kickMessage = config.getNode("kickMessage").getString("");
		Config.async = config.getNode("async").getBoolean();
		if (Config.debug) {
			ServiceLocator.getService(IDebugPrinter.class).printInfo((Config.async) ? "Async enabled" : "Async disabled");
		}
		Config.kick = config.getNode("kick").getBoolean(true);
		if (Config.debug) {
			ServiceLocator.getService(IDebugPrinter.class).printInfo((Config.kick) ? "Set to kick" : "Set to API-only");
		}
	}
	
	public void onEnable() {
		super.onEnable();
		
		swapExceptionHandlers(new RollbarExceptionHandler("dccf7919d6204dfea740702ad41ee08c", "production", version, getServerId(), getDescription().getName()));
		
		List<IMessageHandler> services = ServiceLocator.removeServices(IMessageHandler.class);
		for (IMessageHandler handler : services) {
			try {
				handler.close();
			} catch (Exception ex) {
				
			}
		}
		
		Loaders.loadRedis();
		MessagingLoader.loadMessaging(getDescription().getName(), null, getServerId(), SenderType.PROXY);
		Loaders.loadStorage(getDescription().getName(), getClass().getClassLoader(), getDataFolder());
		
		numCommands = ServiceLocator.getService(CommandProcessor.class).addHandlersFromPackage("me.egg82.avpn.commands", PluginReflectUtil.getCommandMapFromPackage("me.egg82.avpn.commands", false, null, "Command"), false);
		numEvents = ServiceLocator.getService(EventProcessor.class).addHandlersFromPackage("me.egg82.avpn.events");
		if (ServiceLocator.hasService(IMessageHandler.class)) {
			numMessages = ServiceLocator.getService(IMessageHandler.class).addHandlersFromPackage("me.egg82.avpn.messages");
		}
		
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
		
		ThreadUtil.rename(getDescription().getName());
		if (exceptionHandler.hasLimit()) {
			ThreadUtil.schedule(checkExceptionLimitReached, 2L * 60L * 1000L);
		}
		ThreadUtil.schedule(onFetchQueueThread, 10L * 1000L);
	}
	@SuppressWarnings("resource")
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
		
		exceptionHandler.close();
		
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
				IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
				if (handler != null) {
					handler.sendException(ex);
				}
			}
			
			ThreadUtil.schedule(onFetchQueueThread, 10L * 1000L);
		}
	};
	private Runnable checkExceptionLimitReached = new Runnable() {
		public void run() {
			if (exceptionHandler.isLimitReached()) {
				swapExceptionHandlers(new GameAnalyticsExceptionHandler("10b55aa4f41d64ff258f9c66a5fbf9ec", "3794acfebab1122e852d73bbf505c37f42bf3f41", version, getServerId(), getDescription().getName()));
			}
			
			if (exceptionHandler.hasLimit()) {
				ThreadUtil.schedule(checkExceptionLimitReached, 10L * 60L * 1000L);
			}
		}
	};
	
	private void swapExceptionHandlers(IExceptionHandler newHandler) {
		List<IExceptionHandler> oldHandlers = ServiceLocator.removeServices(IExceptionHandler.class);
		
		exceptionHandler = newHandler;
		ServiceLocator.provideService(exceptionHandler);
		
		Logger logger = getLogger();
		if (exceptionHandler instanceof Handler) {
			logger.addHandler((Handler) exceptionHandler);
		}
		
		for (IExceptionHandler handler : oldHandlers) {
			if (handler instanceof Handler) {
				logger.removeHandler((Handler) handler);
			}
			
			handler.close();
			exceptionHandler.addLogs(handler.getUnsentLogs());
		}
	}
	
	private void enableMessage() {
		printInfo(ChatColor.GREEN + "Enabled.");
		printInfo(ChatColor.AQUA + "[Version " + getDescription().getVersion() + "] " + ChatColor.DARK_GREEN + numCommands + " commands " + ChatColor.LIGHT_PURPLE + numEvents + " events " + ChatColor.BLUE + numMessages + " message handlers");
	}
	private void disableMessage() {
		printInfo(ChatColor.RED + "Disabled");
	}
}

package me.egg82.avpn.commands;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import me.egg82.avpn.Config;
import me.egg82.avpn.Loaders;
import me.egg82.avpn.core.RedisSubscriber;
import me.egg82.avpn.debug.IDebugPrinter;
import me.egg82.avpn.registries.CoreConfigRegistry;
import me.egg82.avpn.registries.IPRegistry;
import me.egg82.avpn.utils.RedisUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import ninja.egg82.bungeecord.services.ConfigRegistry;
import ninja.egg82.bungeecord.utils.YamlUtil;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.registries.IVariableRegistry;
import ninja.egg82.plugin.handlers.async.AsyncCommandHandler;
import ninja.egg82.plugin.messaging.IMessageHandler;
import ninja.egg82.sql.ISQL;
import ninja.egg82.utils.FileUtil;
import ninja.egg82.utils.ThreadUtil;
import ninja.egg82.utils.TimeUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class AVPNReloadCommand extends AsyncCommandHandler {
	//vars
	
	//constructor
	public AVPNReloadCommand() {
		super();
	}
	
	//public
	
	//private
	@SuppressWarnings("resource")
	protected void onExecute(long elapsedMilliseconds) {
		if (!sender.hasPermission("avpn.admin")) {
			sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
			return;
		}
		if (args.length != 0) {
			sender.sendMessage(ChatColor.RED + "Incorrect command usage!");
			String name = getClass().getSimpleName();
			name = name.substring(0, name.length() - 7).toLowerCase();
			ServiceLocator.getService(Plugin.class).getProxy().getPluginManager().dispatchCommand((CommandSender) sender.getHandle(), "? " + name);
			return;
		}
		
		// Config
		ServiceLocator.getService(ConfigRegistry.class).load(YamlUtil.getOrLoadDefaults(ServiceLocator.getService(Plugin.class).getDataFolder().getAbsolutePath() + FileUtil.DIRECTORY_SEPARATOR_CHAR + "config.yml", "config.yml", true));
		IVariableRegistry<String> configRegistry = ServiceLocator.getService(ConfigRegistry.class);
		IVariableRegistry<String> coreRegistry = ServiceLocator.getService(CoreConfigRegistry.class);
		coreRegistry.clear();
		for (String key : configRegistry.getKeys()) {
			coreRegistry.setRegister(key, configRegistry.getRegister(key));
		}
		
		Config.debug = configRegistry.getRegister("debug", Boolean.class).booleanValue();
		if (Config.debug) {
			ServiceLocator.getService(IDebugPrinter.class).printInfo("Debug enabled");
		}
		String[] sources = configRegistry.getRegister("sources.order", String.class).split(",\\s?");
		Set<String> sourcesOrdered = new LinkedHashSet<String>();
		for (String source : sources) {
			if (Config.debug) {
				ServiceLocator.getService(IDebugPrinter.class).printInfo("Adding potential source " + source + ".");
			}
			sourcesOrdered.add(source);
		}
		for (Iterator<String> i = sourcesOrdered.iterator(); i.hasNext();) {
			String source = i.next();
			if (!configRegistry.hasRegister("sources." + source + ".enabled")) {
				if (Config.debug) {
					ServiceLocator.getService(IDebugPrinter.class).printInfo(source + " does not have an \"enabled\" flag. Assuming disabled. Removing.");
				}
				i.remove();
				continue;
			}
			if (!configRegistry.getRegister("sources." + source + ".enabled", Boolean.class).booleanValue()) {
				if (Config.debug) {
					ServiceLocator.getService(IDebugPrinter.class).printInfo(source + " is disabled. Removing.");
				}
				i.remove();
				continue;
			}
		}
		Config.sources = ImmutableSet.copyOf(sourcesOrdered);
		Config.sourceCacheTime = TimeUtil.getTime(configRegistry.getRegister("sources.cacheTime", String.class));
		if (Config.debug) {
			ServiceLocator.getService(IDebugPrinter.class).printInfo("Source cache time: " + TimeUtil.timeToHoursMinsSecs(Config.sourceCacheTime));
		}
		Config.kickMessage = configRegistry.getRegister("kickMessage", String.class);
		Config.async = configRegistry.getRegister("async", Boolean.class).booleanValue();
		if (Config.debug) {
			ServiceLocator.getService(IDebugPrinter.class).printInfo((Config.async) ? "Async enabled" : "Async disabled");
		}
		Config.kick = configRegistry.getRegister("kick", Boolean.class).booleanValue();
		if (Config.debug) {
			ServiceLocator.getService(IDebugPrinter.class).printInfo((Config.kick) ? "Set to kick" : "Set to API-only");
		}
		
		// Memory caches
		ServiceLocator.removeServices(IPRegistry.class);
		ServiceLocator.provideService(IPRegistry.class);
		
		// Redis
		JedisPool redisPool = ServiceLocator.getService(JedisPool.class);
		if (redisPool != null) {
			redisPool.close();
		}
		
		Loaders.loadRedis();
		
		ThreadUtil.submit(new Runnable() {
			public void run() {
				try (Jedis redis = RedisUtil.getRedis()) {
					if (redis != null) {
						redis.subscribe(new RedisSubscriber(), "avpn");
					}
				}
			}
		});
		
		// Rabbit
		List<IMessageHandler> services = ServiceLocator.removeServices(IMessageHandler.class);
		for (IMessageHandler handler : services) {
			try {
				handler.close();
			} catch (Exception ex) {
				
			}
		}
		
		Loaders.loadRabbit();
		
		if (ServiceLocator.hasService(IMessageHandler.class)) {
			ServiceLocator.getService(IMessageHandler.class).addHandlersFromPackage("me.egg82.avpn.messages");
		}
		
		// SQL
		List<ISQL> sqls = ServiceLocator.removeServices(ISQL.class);
		for (ISQL sql : sqls) {
			sql.disconnect();
		}
		
		Loaders.loadStorage();
		
		sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
	}
	protected void onUndo() {
		
	}
}

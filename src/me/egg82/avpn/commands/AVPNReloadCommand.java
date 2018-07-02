package me.egg82.avpn.commands;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import com.google.common.collect.ImmutableSet;

import me.egg82.avpn.Config;
import me.egg82.avpn.Loaders;
import me.egg82.avpn.core.RedisSubscriber;
import me.egg82.avpn.registries.IPRegistry;
import me.egg82.avpn.utils.RedisUtil;
import ninja.egg82.bukkit.BasePlugin;
import ninja.egg82.bukkit.services.ConfigRegistry;
import ninja.egg82.bukkit.utils.YamlUtil;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.handlers.CommandHandler;
import ninja.egg82.plugin.messaging.IMessageHandler;
import ninja.egg82.sql.ISQL;
import ninja.egg82.utils.FileUtil;
import ninja.egg82.utils.ThreadUtil;
import ninja.egg82.utils.TimeUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class AVPNReloadCommand extends CommandHandler {
	//vars
	
	//constructor
	public AVPNReloadCommand() {
		super();
	}
	
	//public
	
	//private
	protected void onExecute(long elapsedMilliseconds) {
		if (args.length != 0) {
			sender.sendMessage(ChatColor.RED + "Incorrect command usage!");
			String name = getClass().getSimpleName();
			name = name.substring(0, name.length() - 7).toLowerCase();
			Bukkit.getServer().dispatchCommand((CommandSender) sender.getHandle(), "? " + name);
			return;
		}
		
		// Config
		ServiceLocator.getService(ConfigRegistry.class).load(YamlUtil.getOrLoadDefaults(ServiceLocator.getService(Plugin.class).getDataFolder().getAbsolutePath() + FileUtil.DIRECTORY_SEPARATOR_CHAR + "config.yml", "config.yml", true));
		Config.debug = ServiceLocator.getService(ConfigRegistry.class).getRegister("debug", Boolean.class).booleanValue();
		if (Config.debug) {
			ServiceLocator.getService(BasePlugin.class).printInfo("Debug enabled");
		}
		String[] sources = ServiceLocator.getService(ConfigRegistry.class).getRegister("sources.order", String.class).split(",\\s?");
		Set<String> sourcesOrdered = new LinkedHashSet<String>();
		for (String source : sources) {
			if (Config.debug) {
				ServiceLocator.getService(BasePlugin.class).printInfo("Adding potential source " + source + ".");
			}
			sourcesOrdered.add(source);
		}
		for (Iterator<String> i = sourcesOrdered.iterator(); i.hasNext();) {
			String source = i.next();
			if (!ServiceLocator.getService(ConfigRegistry.class).hasRegister("sources." + source + ".enabled")) {
				if (Config.debug) {
					ServiceLocator.getService(BasePlugin.class).printInfo(source + " does not have an \"enabled\" flag. Assuming disabled. Removing.");
				}
				i.remove();
				continue;
			}
			if (!ServiceLocator.getService(ConfigRegistry.class).getRegister("sources." + source + ".enabled", Boolean.class).booleanValue()) {
				if (Config.debug) {
					ServiceLocator.getService(BasePlugin.class).printInfo(source + " is disabled. Removing.");
				}
				i.remove();
				continue;
			}
		}
		Config.sources = ImmutableSet.copyOf(sourcesOrdered);
		Config.sourceCacheTime = TimeUtil.getTime(ServiceLocator.getService(ConfigRegistry.class).getRegister("sources.cacheTime", String.class));
		if (Config.debug) {
			ServiceLocator.getService(BasePlugin.class).printInfo("Source cache time: " + TimeUtil.timeToHoursMinsSecs(Config.sourceCacheTime));
		}
		Config.kickMessage = ServiceLocator.getService(ConfigRegistry.class).getRegister("kickMessage", String.class);
		Config.async = ServiceLocator.getService(ConfigRegistry.class).getRegister("async", Boolean.class).booleanValue();
		if (Config.debug) {
			ServiceLocator.getService(BasePlugin.class).printInfo((Config.async) ? "Async enabled" : "Async disabled");
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

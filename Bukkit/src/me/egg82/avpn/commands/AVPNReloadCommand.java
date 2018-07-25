package me.egg82.avpn.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

import me.egg82.avpn.Config;
import me.egg82.avpn.Configuration;
import me.egg82.avpn.Loaders;
import me.egg82.avpn.MessagingLoader;
import me.egg82.avpn.core.RedisSubscriber;
import me.egg82.avpn.debug.IDebugPrinter;
import me.egg82.avpn.registries.IPRegistry;
import me.egg82.avpn.utils.RedisUtil;
import ninja.egg82.bukkit.BasePlugin;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.enums.SenderType;
import ninja.egg82.plugin.handlers.CommandHandler;
import ninja.egg82.plugin.messaging.IMessageHandler;
import ninja.egg82.plugin.utils.DirectoryUtil;
import ninja.egg82.sql.ISQL;
import ninja.egg82.utils.ThreadUtil;
import ninja.egg82.utils.TimeUtil;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
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
	@SuppressWarnings("resource")
	protected void onExecute(long elapsedMilliseconds) {
		if (args.length != 0) {
			sender.sendMessage(ChatColor.RED + "Incorrect command usage!");
			String name = getClass().getSimpleName();
			name = name.substring(0, name.length() - 7).toLowerCase();
			Bukkit.getServer().dispatchCommand((CommandSender) sender.getHandle(), "? " + name);
			return;
		}
		
		// Config
		File configFile = new File(ServiceLocator.getService(Plugin.class).getDataFolder(), "config.yml");
		if (configFile.exists() && configFile.isDirectory()) {
			DirectoryUtil.delete(configFile);
		}
		if (!configFile.exists()) {
			try (InputStreamReader reader = new InputStreamReader(ServiceLocator.getService(Plugin.class).getResource("config.yml")); BufferedReader in = new BufferedReader(reader); FileWriter writer = new FileWriter(configFile); BufferedWriter out = new BufferedWriter(writer)) {
				while (in.ready()) {
					writer.write(in.readLine());
				}
			} catch (Exception ex) {
				
			}
		}
		
		ConfigurationLoader<ConfigurationNode> loader = YAMLConfigurationLoader.builder().setIndent(2).setFile(configFile).build();
		ConfigurationNode root = null;
		try {
			root = loader.load();
		} catch (Exception ex) {
			throw new RuntimeException("Error loading config. Aborting plugin load.", ex);
		}
		Configuration config = new Configuration(root);
		ServiceLocator.removeServices(Configuration.class);
		ServiceLocator.provideService(config);
		
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
		Config.kick = config.getNode("kick").getBoolean(true);
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
		
		MessagingLoader.loadMessaging(ServiceLocator.getService(Plugin.class).getDescription().getName(), Bukkit.getServerName(), ServiceLocator.getService(BasePlugin.class).getServerId(), SenderType.SERVER);
		
		if (ServiceLocator.hasService(IMessageHandler.class)) {
			ServiceLocator.getService(IMessageHandler.class).addHandlersFromPackage("me.egg82.avpn.messages");
		}
		
		// SQL
		List<ISQL> sqls = ServiceLocator.removeServices(ISQL.class);
		for (ISQL sql : sqls) {
			sql.disconnect();
		}
		
		Loaders.loadStorage(ServiceLocator.getService(Plugin.class).getDescription().getName(), ServiceLocator.getService(Plugin.class).getClass().getClassLoader(), ServiceLocator.getService(Plugin.class).getDataFolder());
		
		sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
	}
	protected void onUndo() {
		
	}
}

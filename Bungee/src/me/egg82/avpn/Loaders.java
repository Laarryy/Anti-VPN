package me.egg82.avpn;

import java.io.File;

import me.egg82.avpn.sql.mysql.CreateTablesMySQLCommand;
import me.egg82.avpn.sql.mysql.LoadInfoMySQLCommand;
import me.egg82.avpn.sql.sqlite.CreateTablesSQLiteCommand;
import me.egg82.avpn.sql.sqlite.LoadInfoSQLiteCommand;
import ninja.egg82.bungeecord.BasePlugin;
import ninja.egg82.bungeecord.messaging.EnhancedBungeeMessageHandler;
import ninja.egg82.bungeecord.services.ConfigRegistry;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.registries.IVariableRegistry;
import ninja.egg82.plugin.enums.SenderType;
import ninja.egg82.plugin.messaging.IMessageHandler;
import ninja.egg82.plugin.messaging.RabbitMessageHandler;
import ninja.egg82.sql.ISQL;
import ninja.egg82.sql.MySQL;
import ninja.egg82.sql.SQLite;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Loaders {
	//vars
	
	//constructor
	public Loaders() {
		
	}
	
	//public
	public static void loadStorage() {
		BasePlugin plugin = ServiceLocator.getService(BasePlugin.class);
		IVariableRegistry<String> configRegistry = ServiceLocator.getService(ConfigRegistry.class);
		ISQL sql = null;
		
		if (configRegistry.hasRegister("sql.type") && configRegistry.hasRegister("sql.threads")) {
			String type = configRegistry.getRegister("sql.type", String.class);
			int threads = configRegistry.getRegister("sql.threads", Number.class).intValue();
			
			if (type.equalsIgnoreCase("mysql")) {
				if (
					configRegistry.hasRegister("sql.mysql.address")
					&& configRegistry.hasRegister("sql.mysql.port")
					&& configRegistry.hasRegister("sql.mysql.user")
					&& configRegistry.hasRegister("sql.mysql.database")
				) {
					sql = new MySQL(threads, plugin.getDescription().getName(), plugin.getClass().getClassLoader());
					sql.connect(
						configRegistry.getRegister("sql.mysql.address", String.class),
						configRegistry.getRegister("sql.mysql.port", Number.class).intValue(),
						configRegistry.getRegister("sql.mysql.user", String.class),
						configRegistry.hasRegister("sql.mysql.pass") ? configRegistry.getRegister("sql.mysql.pass", String.class) : "",
						configRegistry.getRegister("sql.mysql.database", String.class)
					);
					ServiceLocator.provideService(sql);
					new CreateTablesMySQLCommand().start();
					new LoadInfoMySQLCommand().start();
				} else {
					throw new RuntimeException("\"sql.mysql.address\", \"sql.mysql.port\", \"sql.mysql.user\", or \"sql.mysql.database\" missing from config. Aborting plugin load.");
				}
			} else if (type.equalsIgnoreCase("sqlite")) {
				if (
					configRegistry.hasRegister("sql.sqlite.file")
				) {
					sql = new SQLite(threads, plugin.getDescription().getName(), plugin.getClass().getClassLoader());
					sql.connect(new File(plugin.getDataFolder(), configRegistry.getRegister("sql.sqlite.file", String.class)).getAbsolutePath());
					ServiceLocator.provideService(sql);
					new CreateTablesSQLiteCommand().start();
					new LoadInfoSQLiteCommand().start();
				} else {
					throw new RuntimeException("\"sql.sqlite.file\" missing from config. Aborting plugin load.");
				}
			} else {
				throw new RuntimeException("\"sql.type\" was neither 'mysql' nor 'sqlite'. Aborting plugin load.");
			}
		} else {
			throw new RuntimeException("\"sql.type\" or \"sql.threads\" missing from config. Aborting plugin load.");
		}
	}
	@SuppressWarnings("resource")
	public static void loadRedis() {
		IVariableRegistry<String> configRegistry = ServiceLocator.getService(ConfigRegistry.class);
		
		if (configRegistry.hasRegister("redis.enabled")) {
			if (configRegistry.getRegister("redis.enabled", Boolean.class).booleanValue()) {
				if (
					configRegistry.hasRegister("redis.address")
					&& configRegistry.hasRegister("redis.port")
				) {
					JedisPoolConfig redisPoolConfig = new JedisPoolConfig();
					redisPoolConfig.setMaxTotal(16);
					redisPoolConfig.setMaxIdle(2);
					redisPoolConfig.setBlockWhenExhausted(false);
					redisPoolConfig.setTestOnBorrow(false);
					redisPoolConfig.setTestOnCreate(false);
					redisPoolConfig.setTestOnReturn(false);
					redisPoolConfig.setTestWhileIdle(true);
					redisPoolConfig.setMaxWaitMillis(30000L);
					JedisPool redisPool = new JedisPool(
						redisPoolConfig,
						configRegistry.getRegister("redis.address", String.class),
						configRegistry.getRegister("redis.port", Number.class).intValue()
					);
					ServiceLocator.provideService(redisPool);
				} else {
					throw new RuntimeException("\"redis.address\" or \"redis.port\" missing from config. Aborting plugin load.");
				}
			}
		} else {
			throw new RuntimeException("\"redis.enabled\" missing from config. Aborting plugin load.");
		}
	}
	@SuppressWarnings("resource")
	public static void loadRabbit() {
		BasePlugin plugin = ServiceLocator.getService(BasePlugin.class);
		IVariableRegistry<String> configRegistry = ServiceLocator.getService(ConfigRegistry.class);
		
		if (configRegistry.hasRegister("rabbit.enabled")) {
			if (configRegistry.getRegister("rabbit.enabled", Boolean.class).booleanValue()) {
				if (
					configRegistry.hasRegister("rabbit.address")
					&& configRegistry.hasRegister("rabbit.port")
					&& configRegistry.hasRegister("rabbit.user")
				) {
					ServiceLocator.provideService(
						new RabbitMessageHandler(
							configRegistry.getRegister("rabbit.address", String.class),
							configRegistry.getRegister("rabbit.port", Number.class).intValue(),
							configRegistry.getRegister("rabbit.user", String.class),
							configRegistry.hasRegister("rabbit.pass") ? configRegistry.getRegister("rabbit.pass", String.class) : "",
							plugin.getDescription().getName(),
							plugin.getServerId(),
							SenderType.PROXY
						)
					);
				} else {
					throw new RuntimeException("\"rabbit.address\", \"rabbit.port\", or \"rabbit.user\" missing from config. Aborting plugin load.");
				}
			} else {
				if (!ServiceLocator.hasService(EnhancedBungeeMessageHandler.class)) {
					ServiceLocator.provideService(new EnhancedBungeeMessageHandler(plugin.getDescription().getName(), plugin.getServerId()));
				}
			}
		} else {
			throw new RuntimeException("\"rabbit.enabled\" missing from config. Aborting plugin load.");
		}
		
		IMessageHandler messageHandler = ServiceLocator.getService(IMessageHandler.class);
		messageHandler.setSenderId(plugin.getServerId());
		
		messageHandler.createChannel("AntiVPNIPInfo");
	}
	
	//private
	
}

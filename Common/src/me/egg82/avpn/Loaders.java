package me.egg82.avpn;

import java.io.File;

import me.egg82.avpn.sql.mysql.CreateTablesMySQLCommand;
import me.egg82.avpn.sql.mysql.LoadInfoMySQLCommand;
import me.egg82.avpn.sql.sqlite.CreateTablesSQLiteCommand;
import me.egg82.avpn.sql.sqlite.LoadInfoSQLiteCommand;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.sql.ISQL;
import ninja.egg82.sql.MySQL;
import ninja.egg82.sql.SQLite;
import ninja.egg82.utils.ThreadUtil;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Loaders {
	//vars
	
	//constructor
	public Loaders() {
		
	}
	
	//public
	public static void loadStorage(String pluginName, ClassLoader pluginClassLoader, File pluginDataFolder) {
		if (pluginName == null) {
			throw new IllegalArgumentException("pluginName cannot be null.");
		}
		if (pluginDataFolder == null) {
			throw new IllegalArgumentException("pluginDataFolder cannot be null.");
		}
		
		Configuration config = ServiceLocator.getService(Configuration.class);
		ISQL sql = null;
		
		String type = config.getNode("sql", "type").getString("sqlite");
		int threads = config.getNode("sql", "threads").getInt(2);
		
		if (type.equalsIgnoreCase("mysql")) {
			sql = new MySQL(threads, pluginName, pluginClassLoader);
			sql.connect(
				config.getNode("sql", "mysql", "address").getString("127.0.0.1"),
				config.getNode("sql", "mysql", "port").getInt(3306),
				config.getNode("sql", "mysql", "user").getString(""),
				config.getNode("sql", "mysql", "pass").getString(""),
				config.getNode("sql", "mysql", "database").getString("antivpn")
			);
			ServiceLocator.provideService(sql);
			new CreateTablesMySQLCommand().start();
			ThreadUtil.schedule(new Runnable() {
				public void run() {
					new LoadInfoMySQLCommand().start();
				}
			}, 1000L);
		} else if (type.equalsIgnoreCase("sqlite")) {
			sql = new SQLite(threads, pluginName, pluginClassLoader);
			sql.connect(new File(pluginDataFolder, config.getNode("sql", "sqlite", "file").getString()).getAbsolutePath());
			ServiceLocator.provideService(sql);
			new CreateTablesSQLiteCommand().start();
			ThreadUtil.schedule(new Runnable() {
				public void run() {
					new LoadInfoSQLiteCommand().start();
				}
			}, 1000L);
		} else {
			throw new RuntimeException("\"sql.type\" was neither 'mysql' nor 'sqlite'. Aborting plugin load.");
		}
	}
	@SuppressWarnings("resource")
	public static void loadRedis() {
		Configuration config = ServiceLocator.getService(Configuration.class);
		
		if (config.getNode("redis", "enabled").getBoolean()) {
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
				config.getNode("redis", "address").getString("127.0.0.1"),
				config.getNode("redis", "port").getInt(6379)
			);
			ServiceLocator.provideService(redisPool);
		}
	}
	
	//private
	
}

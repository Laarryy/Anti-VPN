package me.egg82.avpn.sql.sqlite;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.BiConsumer;

import me.egg82.avpn.Config;
import me.egg82.avpn.utils.RedisUtil;
import me.egg82.avpn.utils.ValidationUtil;
import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.events.SQLEventArgs;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.Command;
import ninja.egg82.sql.ISQL;
import redis.clients.jedis.Jedis;

public class LoadInfoSQLiteCommand extends Command {
	//vars
	private ISQL sql = ServiceLocator.getService(ISQL.class);
	
	private UUID query = null;
	
	private BiConsumer<Object, SQLEventArgs> sqlError = (s, e) -> onSQLError(e);
	private BiConsumer<Object, SQLEventArgs> sqlData = (s, e) -> onSQLData(e);
	
	//constructor
	public LoadInfoSQLiteCommand() {
		super();
		
		sql.onError().attach(sqlError);
		sql.onData().attach(sqlData);
	}
	
	//public
	
	//private
	protected void onExecute(long elapsedMilliseconds) {
		query = sql.parallelQuery("SELECT `ip`, `value`, `created` FROM `antivpn`;");
	}
	
	private void onSQLData(SQLEventArgs e) {
		if (e.getUuid().equals(query)) {
			Exception lastEx = null;
			
			try (Jedis redis = RedisUtil.getRedis()) {
				// Iterate rows
				for (Object[] o : e.getData().data) {
					try {
						// Validate IP and remove bad data
						if (!ValidationUtil.isValidIp((String) o[0])) {
							if (redis != null) {
								String key = "avpn:" + (String) o[0];
								redis.del(key);
							}
							sql.parallelQuery("DELETE FROM `antivpn` WHERE `ip`=?;", o[0]);
							
							continue;
						}
						
						// Grab all data and convert to more useful object types
						String ip = (String) o[0];
						boolean value = (((Number) o[1]).intValue() == 0) ? false : true;
						long created = Timestamp.valueOf((String) o[2]).getTime();
						
						// Set Redis, if available
						if (redis != null) {
							String key = "avpn:" + ip;
							int offset = (int) Math.floorDiv((Config.sourceCacheTime + created) - System.currentTimeMillis(), 1000L);
							if (offset > 0) {
								redis.setex(key, offset, String.valueOf(value));
							} else {
								redis.del(key);
							}
						}
					} catch (Exception ex) {
						IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
						if (handler != null) {
							handler.sendException(ex);
						}
						ex.printStackTrace();
						lastEx = ex;
					}
				}
			}
			
			sql.onError().detatch(sqlError);
			sql.onData().detatch(sqlError);
			
			if (lastEx != null) {
				throw new RuntimeException(lastEx);
			}
		}
	}
	private void onSQLError(SQLEventArgs e) {
		if (!e.getUuid().equals(query)) {
			return;
		}
		
		IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
		if (handler != null) {
			handler.sendException(e.getSQLError().ex);
		}
		// Wrap in a new exception and print to console. We wrap so we know where the error actually comes from
		new Exception(e.getSQLError().ex).printStackTrace();
		
		sql.onError().detatch(sqlError);
		sql.onData().detatch(sqlError);
		
		throw new RuntimeException(e.getSQLError().ex);
	}
}

package me.egg82.avpn;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import me.egg82.avpn.apis.GetIPIntelAPI;
import me.egg82.avpn.apis.IPDetectorAPI;
import me.egg82.avpn.apis.IPHubAPI;
import me.egg82.avpn.apis.ProxyCheckAPI;
import me.egg82.avpn.apis.ShodanAPI;
import me.egg82.avpn.apis.VPNBlockerAPI;
import me.egg82.avpn.core.ResultEventArgs;
import me.egg82.avpn.registries.IPRegistry;
import me.egg82.avpn.registries.InvalidRegistry;
import me.egg82.avpn.sql.mysql.SelectResultMySQLCommand;
import me.egg82.avpn.sql.sqlite.SelectResultSQLiteCommand;
import me.egg82.avpn.utils.IPCacheUtil;
import me.egg82.avpn.utils.RedisUtil;
import me.egg82.avpn.utils.ValidationUtil;
import ninja.egg82.bukkit.BasePlugin;
import ninja.egg82.bukkit.services.ConfigRegistry;
import ninja.egg82.enums.BaseSQLType;
import ninja.egg82.exceptionHandlers.IExceptionHandler;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.registries.IExpiringRegistry;
import ninja.egg82.patterns.registries.IRegistry;
import ninja.egg82.sql.ISQL;
import redis.clients.jedis.Jedis;

public class VPNAPI {
	//vars
	
	//constructor
	public VPNAPI() {
		
	}
	
	//public
	public static VPNAPI getInstance() {
		return AntiVPN.getAPI();
	}
	
	public boolean isVPN(String ip) {
		return isVPN(ip, true);
	}
	public boolean isVPN(String ip, boolean expensive) {
		if (ip == null) {
			throw new IllegalArgumentException("ip cannot be null.");
		}
		if (!ValidationUtil.isValidIp(ip)) {
			return false;
		}
		
		if (Config.debug) {
			ServiceLocator.getService(BasePlugin.class).printInfo("Checking IP " + ip);
		}
		
		// Internal cache - use first
		IExpiringRegistry<String, Boolean> ipRegistry = ServiceLocator.getService(IPRegistry.class);
		Boolean bool = ipRegistry.getRegister(ip);
		if (bool != null) {
			if (Config.debug) {
				ServiceLocator.getService(BasePlugin.class).printInfo(ip + " found in local cache. Value: " + bool.toString());
			}
			return bool.booleanValue();
		}
		
		// Redis - use BEFORE SQL
		try (Jedis redis = RedisUtil.getRedis()) {
			if (redis != null) {
				String key = "avpn:" + ip;
				
				// Grab IP info
				String data = redis.get(key);
				if (data != null) {
					bool = Boolean.valueOf(data);
				}
			}
		}
		
		if (bool != null) {
			// Redis returned some data. Cache the result
			ipRegistry.setRegister(ip, bool);
			if (Config.debug) {
				ServiceLocator.getService(BasePlugin.class).printInfo(ip + " found in Redis. Value: " + bool.toString());
			}
			return bool.booleanValue();
		}
		
		// SQL - use BEFORE lookup
		AtomicReference<ResultEventArgs> retVal = new AtomicReference<ResultEventArgs>(null);
		CountDownLatch latch = new CountDownLatch(1);
		
		BiConsumer<Object, ResultEventArgs> sqlData = (s, e) -> {
			retVal.set(e);
			latch.countDown();
			
			ISQL sql = ServiceLocator.getService(ISQL.class);
			if (sql.getType() == BaseSQLType.MySQL) {
				SelectResultMySQLCommand c = (SelectResultMySQLCommand) s;
				c.onData().detatchAll();
			} else if (sql.getType() == BaseSQLType.SQLite) {
				SelectResultSQLiteCommand c = (SelectResultSQLiteCommand) s;
				c.onData().detatchAll();
			}
		};
		
		ISQL sql = ServiceLocator.getService(ISQL.class);
		if (sql.getType() == BaseSQLType.MySQL) {
			SelectResultMySQLCommand command = new SelectResultMySQLCommand(ip);
			command.onData().attach(sqlData);
			command.start();
		} else if (sql.getType() == BaseSQLType.SQLite) {
			SelectResultSQLiteCommand command = new SelectResultSQLiteCommand(ip);
			command.onData().attach(sqlData);
			command.start();
		}
		
		try {
			latch.await();
		} catch (Exception ex) {
			ServiceLocator.getService(IExceptionHandler.class).silentException(ex);
			ex.printStackTrace();
		}
		
		if (retVal.get() != null && retVal.get().getIp() != null && retVal.get().getValue() != null) {
			// Set Redis, if available
			try (Jedis redis = RedisUtil.getRedis()) {
				if (redis != null) {
					String key = "avpn:" + ip;
					int offset = (int) Math.floorDiv((Config.sourceCacheTime + retVal.get().getCreated()) - System.currentTimeMillis(), 1000L);
					if (offset > 0) {
						redis.setex(key, offset, retVal.get().getValue().toString());
					} else {
						redis.del(key);
					}
				}
			}
			
			bool = retVal.get().getValue();
			// Cache the result
			ipRegistry.setRegister(ip, bool);
			
			if (Config.debug) {
				ServiceLocator.getService(BasePlugin.class).printInfo(ip + " found in SQL. Value: " + bool.toString());
			}
			return bool.booleanValue();
		}
		
		if (!expensive) {
			// Non-expensive call. Return false, but don't cache this result
			return false;
		}
		
		// Lookup via APIs - use as last resort
		IRegistry<String, Boolean> invalidRegistry = ServiceLocator.getService(InvalidRegistry.class);
		for (String source : Config.sources) {
			if (invalidRegistry.hasRegister(source)) {
				if (Config.debug) {
					ServiceLocator.getService(BasePlugin.class).printInfo("Skipping " + source + " for " + ip + " due to recently bad/failed check.");
				}
				continue;
			}
			
			if (Config.debug) {
				ServiceLocator.getService(BasePlugin.class).printInfo("Trying " + source + " as next source for " + ip + ".");
			}
			
			if (source.equalsIgnoreCase("getipintel")) {
				Double result = GetIPIntelAPI.getResult(ip);
				if (result != null) {
					bool = (result.doubleValue() >= ServiceLocator.getService(ConfigRegistry.class).getRegister("sources.getipintel.threshold", Number.class).doubleValue()) ? Boolean.TRUE : Boolean.FALSE;
				}
			} else if (source.equalsIgnoreCase("vpnblocker")) {
				bool = VPNBlockerAPI.isVPN(ip);
			} else if (source.equalsIgnoreCase("ipdetector")) {
				bool = IPDetectorAPI.isVPN(ip);
			} else if (source.equalsIgnoreCase("iphub")) {
				bool = IPHubAPI.isVPN(ip);
			} else if (source.equalsIgnoreCase("proxycheck")) {
				bool = ProxyCheckAPI.isVPN(ip);
			} else if (source.equalsIgnoreCase("shodan")) {
				bool = ShodanAPI.isVPN(ip);
			}
			
			if (bool != null) {
				break;
			}
			
			if (Config.debug) {
				ServiceLocator.getService(BasePlugin.class).printInfo(source + " returned a bad/failed result for " + ip + ". Skipping source for a while.");
			}
			invalidRegistry.setRegister(source, Boolean.TRUE);
		}
		
		if (bool == null) {
			// Something went wrong. return false, but don't cache this
			if (Config.debug) {
				ServiceLocator.getService(BasePlugin.class).printInfo(ip + " Was not able to be fetched. Returning \"false\" as default value.");
			}
			return false;
		}
		
		// Add the result
		IPCacheUtil.addInfo(ip, bool.booleanValue());
		
		if (Config.debug) {
			ServiceLocator.getService(BasePlugin.class).printInfo(ip + " fetched via defined sources. Value: " + bool.toString());
		}
		return bool.booleanValue();
	}
	
	//private
	
}

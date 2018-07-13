package me.egg82.avpn.utils;

import me.egg82.avpn.registries.CoreConfigRegistry;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.registries.IVariableRegistry;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisUtil {
	//vars
	
	//constructor
	public RedisUtil() {
		
	}
	
	//public
	@SuppressWarnings("resource")
	public static Jedis getRedis() {
		Jedis redis = null;
		
		IVariableRegistry<String> configRegistry = ServiceLocator.getService(CoreConfigRegistry.class);
		JedisPool redisPool = ServiceLocator.getService(JedisPool.class);
		if (redisPool != null) {
			redis = redisPool.getResource();
			if (configRegistry.hasRegister("redis.pass") && !configRegistry.getRegister("redis.pass", String.class).isEmpty()) {
				redis.auth(configRegistry.getRegister("redis.pass", String.class));
			}
		}
		
		return redis;
	}
	
	//private
	
}

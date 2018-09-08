package me.egg82.avpn.utils;

import me.egg82.avpn.Configuration;
import me.egg82.avpn.debug.IDebugPrinter;
import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.patterns.ServiceLocator;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisUtil {
    // vars

    // constructor
    public RedisUtil() {

    }

    // public
    @SuppressWarnings("resource")
    public static Jedis getRedis() {
        Jedis redis = null;

        Configuration config = ServiceLocator.getService(Configuration.class);
        JedisPool redisPool = ServiceLocator.getService(JedisPool.class);
        if (redisPool != null) {
            try {
                redis = redisPool.getResource();
            } catch (Exception ex) {
                IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
                if (handler != null) {
                    handler.sendException(ex);
                }
                ServiceLocator.getService(IDebugPrinter.class).printError("Could not get Redis connection. Is the config properly set and the Redis server accepting connections?");
            }

            if (redis != null) {
                String pass = config.getNode("redis", "pass").getString();
                if (pass != null && !pass.isEmpty()) {
                    redis.auth(pass);
                }
            }
        }

        return redis;
    }

    // private

}

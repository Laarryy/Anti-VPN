package me.egg82.antivpn.utils;

import ninja.leaping.configurate.ConfigurationNode;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

public class RedisUtil {
    private RedisUtil() {}

    public static Jedis getRedis(JedisPool pool, ConfigurationNode redisConfigNode) throws JedisException {
        if (redisConfigNode == null) {
            throw new IllegalArgumentException("redisConfigNode cannot be null.");
        }

        Jedis redis = null;

        if (pool != null) {
            redis = pool.getResource();
            String pass = redisConfigNode.getNode("password").getString();
            if (pass != null && !pass.isEmpty()) {
                redis.auth(pass);
            }
        }

        return redis;
    }
}

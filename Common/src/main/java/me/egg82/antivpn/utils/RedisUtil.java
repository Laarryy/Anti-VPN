package me.egg82.antivpn.utils;

import java.util.Optional;
import me.egg82.antivpn.extended.CachedConfigValues;
import ninja.leaping.configurate.ConfigurationNode;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

public class RedisUtil {
    private RedisUtil() {}

    public static Jedis getRedis() throws JedisException {
        ConfigurationNode redisConfigNode = ConfigUtil.getRedisNodeOrNull();
        if (redisConfigNode == null) {
            return null;
        }

        Optional<CachedConfigValues> configValues = ConfigUtil.getCachedConfig();
        if (!configValues.isPresent()) {
            return null;
        }

        Jedis redis = null;

        if (configValues.get().getRedisPool() != null) {
            redis = configValues.get().getRedisPool().getResource();
            String pass = redisConfigNode.getNode("password").getString();
            if (pass != null && !pass.isEmpty()) {
                redis.auth(pass);
            }
        }

        return redis;
    }
}

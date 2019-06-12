package me.egg82.antivpn.services;

import java.util.Optional;
import java.util.UUID;
import me.egg82.antivpn.core.ConsensusResult;
import me.egg82.antivpn.core.DataResult;
import me.egg82.antivpn.core.SQLFetchResult;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.RedisUtil;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

public class Redis {
    private static final Logger logger = LoggerFactory.getLogger(Redis.class);

    private static final UUID serverId = UUID.randomUUID();
    public static UUID getServerID() { return serverId; }

    private Redis() {}

    public static void updateFromQueue(SQLFetchResult sqlResult) {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            return;
        }

        try (Jedis redis = RedisUtil.getRedis()) {
            if (redis == null) {
                return;
            }

            for (String key : sqlResult.getRemovedKeys()) {
                redis.del(key);
                redis.publish("antivpn-delete", key);
            }

            for (DataResult result : sqlResult.getData()) {
                String key = "avpn:" + result.getIp();
                int offset = (int) Math.floorDiv((cachedConfig.get().getSourceCacheTime() + result.getCreated()) - System.currentTimeMillis(), 1000L);
                if (offset > 0) {
                    redis.setex(key, offset, String.valueOf(result.getValue()));
                } else {
                    redis.del(key);
                }

                if (offset > 0) {
                    JSONObject obj = new JSONObject();
                    obj.put("ip", result.getIp());
                    obj.put("value", result.getValue());
                    obj.put("created", result.getCreated());
                    obj.put("id", serverId.toString());
                    redis.publish("antivpn-result", obj.toJSONString());
                } else {
                    redis.publish("antivpn-delete", result.getIp());
                }
            }

            for (ConsensusResult result : sqlResult.getConsensus()) {
                String key = "avpn:consensus:" + result.getIp();
                int offset = (int) Math.floorDiv((cachedConfig.get().getSourceCacheTime() + result.getCreated()) - System.currentTimeMillis(), 1000L);
                if (offset > 0) {
                    redis.setex(key, offset, String.valueOf(result.getValue()));
                } else {
                    redis.del(key);
                }

                if (offset > 0) {
                    JSONObject obj = new JSONObject();
                    obj.put("ip", result.getIp());
                    obj.put("value", result.getValue());
                    obj.put("created", result.getCreated());
                    obj.put("id", serverId.toString());
                    redis.publish("antivpn-consensus", obj.toJSONString());
                } else {
                    redis.publish("antivpn-delete", result.getIp());
                }
            }
        } catch (JedisException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public static void update(DataResult sqlResult) {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            return;
        }

        try (Jedis redis = RedisUtil.getRedis()) {
            if (redis == null) {
                return;
            }

            String key = "avpn:" + sqlResult.getIp();
            int offset = (int) Math.floorDiv((cachedConfig.get().getSourceCacheTime() + sqlResult.getCreated()) - System.currentTimeMillis(), 1000L);
            if (offset > 0) {
                redis.setex(key, offset, String.valueOf(sqlResult.getValue()));
            } else {
                redis.del(key);
            }

            if (offset > 0) {
                JSONObject obj = new JSONObject();
                obj.put("ip", sqlResult.getIp());
                obj.put("value", sqlResult.getValue());
                obj.put("created", sqlResult.getCreated());
                obj.put("id", serverId.toString());
                redis.publish("antivpn-result", obj.toJSONString());
            } else {
                redis.publish("antivpn-delete", sqlResult.getIp());
            }
        } catch (JedisException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public static void update(ConsensusResult sqlResult) {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            return;
        }

        try (Jedis redis = RedisUtil.getRedis()) {
            if (redis == null) {
                return;
            }

            String key = "avpn:consensus:" + sqlResult.getIp();
            int offset = (int) Math.floorDiv((cachedConfig.get().getSourceCacheTime() + sqlResult.getCreated()) - System.currentTimeMillis(), 1000L);
            if (offset > 0) {
                redis.setex(key, offset, String.valueOf(sqlResult.getValue()));
            } else {
                redis.del(key);
            }

            if (offset > 0) {
                JSONObject obj = new JSONObject();
                obj.put("ip", sqlResult.getIp());
                obj.put("value", sqlResult.getValue());
                obj.put("created", sqlResult.getCreated());
                obj.put("id", serverId.toString());
                redis.publish("antivpn-consensus", obj.toJSONString());
            } else {
                redis.publish("antivpn-delete", sqlResult.getIp());
            }
        } catch (JedisException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public static Optional<Boolean> getResult(String ip) {
        Boolean result = null;

        try (Jedis redis = RedisUtil.getRedis()) {
            if (redis != null) {
                String key = "avpn:" + ip;

                // Grab IP info
                String data = redis.get(key);
                if (data != null) {
                    result = Boolean.valueOf(data);
                }
            }
        } catch (JedisException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return Optional.ofNullable(result);
    }

    public static Optional<Double> getConsensus(String ip) {
        Double result = null;

        try (Jedis redis = RedisUtil.getRedis()) {
            if (redis != null) {
                String key = "avpn:consensus:" + ip;

                // Grab IP info
                String data = redis.get(key);
                if (data != null) {
                    result = Double.valueOf(data);
                }
            }
        } catch (JedisException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return Optional.ofNullable(result);
    }
}

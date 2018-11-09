package me.egg82.antivpn.services;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.core.ConsensusResult;
import me.egg82.antivpn.core.DataResult;
import me.egg82.antivpn.core.SQLFetchResult;
import me.egg82.antivpn.utils.RedisUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

public class Redis {
    private static final Logger logger = LoggerFactory.getLogger(Redis.class);

    private static final UUID serverId = UUID.randomUUID();
    public static UUID getServerID() { return serverId; }

    private Redis() {}

    public static CompletableFuture<Boolean> updateFromQueue(SQLFetchResult sqlResult, long sourceCacheTime, JedisPool pool, ConfigurationNode redisConfigNode) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis redis = RedisUtil.getRedis(pool, redisConfigNode)) {
                if (redis == null) {
                    return Boolean.FALSE;
                }

                for (DataResult result : sqlResult.getData()) {
                    String key = "avpn:" + result.getIp();
                    int offset = (int) Math.floorDiv((sourceCacheTime + result.getCreated()) - System.currentTimeMillis(), 1000L);
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
                    int offset = (int) Math.floorDiv((sourceCacheTime + result.getCreated()) - System.currentTimeMillis(), 1000L);
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

                for (String key : sqlResult.getRemovedKeys()) {
                    redis.del(key);
                    redis.publish("antivpn-delete", key);
                }

                return Boolean.TRUE;
            } catch (JedisException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return Boolean.FALSE;
        });
    }

    public static CompletableFuture<Boolean> update(DataResult sqlResult, long sourceCacheTime, JedisPool pool, ConfigurationNode redisConfigNode) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis redis = RedisUtil.getRedis(pool, redisConfigNode)) {
                if (redis == null) {
                    return Boolean.FALSE;
                }

                String key = "avpn:" + sqlResult.getIp();
                int offset = (int) Math.floorDiv((sourceCacheTime + sqlResult.getCreated()) - System.currentTimeMillis(), 1000L);
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

                return Boolean.TRUE;
            } catch (JedisException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return Boolean.FALSE;
        });
    }

    public static CompletableFuture<Boolean> update(ConsensusResult sqlResult, long sourceCacheTime, JedisPool pool, ConfigurationNode redisConfigNode) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis redis = RedisUtil.getRedis(pool, redisConfigNode)) {
                if (redis == null) {
                    return Boolean.FALSE;
                }

                String key = "avpn:consensus:" + sqlResult.getIp();
                int offset = (int) Math.floorDiv((sourceCacheTime + sqlResult.getCreated()) - System.currentTimeMillis(), 1000L);
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

                return Boolean.TRUE;
            } catch (JedisException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return Boolean.FALSE;
        });
    }

    public static CompletableFuture<Boolean> getResult(String ip, JedisPool pool, ConfigurationNode redisConfigNode) {
        return CompletableFuture.supplyAsync(() -> {
            Boolean result = null;

            try (Jedis redis = RedisUtil.getRedis(pool, redisConfigNode)) {
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

            return result;
        });
    }

    public static CompletableFuture<Double> getConsensus(String ip, JedisPool pool, ConfigurationNode redisConfigNode) {
        return CompletableFuture.supplyAsync(() -> {
            Double result = null;

            try (Jedis redis = RedisUtil.getRedis(pool, redisConfigNode)) {
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

            return result;
        });
    }
}

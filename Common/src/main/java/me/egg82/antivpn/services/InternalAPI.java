package me.egg82.antivpn.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AtomicDouble;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.rabbitmq.client.Connection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import me.egg82.antivpn.apis.API;
import me.egg82.antivpn.core.ConsensusResult;
import me.egg82.antivpn.core.DataResult;
import me.egg82.antivpn.enums.SQLType;
import me.egg82.antivpn.sql.MySQL;
import me.egg82.antivpn.sql.SQLite;
import ninja.egg82.reflect.PackageFilter;
import ninja.egg82.sql.SQL;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

public class InternalAPI {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static Cache<String, Boolean> ipCache = Caffeine.newBuilder().expireAfterAccess(1L, TimeUnit.MINUTES).expireAfterWrite(6L, TimeUnit.HOURS).build();
    private static Cache<String, Double> ipConsensusCache = Caffeine.newBuilder().expireAfterAccess(1L, TimeUnit.MINUTES).expireAfterWrite(6L, TimeUnit.HOURS).build();
    private static LoadingCache<String, Boolean> sourceValidationCache = Caffeine.newBuilder().expireAfterWrite(1L,TimeUnit.MINUTES).build(k -> Boolean.TRUE);

    private ImmutableMap<String, API> apis;

    private static ExecutorService threadPool = Executors.newWorkStealingPool(4);

    public InternalAPI() {
        ImmutableMap.Builder<String, API> apiBuilder = ImmutableMap.builder();

        List<Class<API>> list = PackageFilter.getClasses(API.class, "me.egg82.antivpn.apis", false, false, false);
        for (Class<API> clazz : list) {
            try {
                API api = clazz.newInstance();
                apiBuilder.put(api.getName(), api);
            } catch (InstantiationException | IllegalAccessException ex) {
                logger.error(ex.getMessage(), ex);
                throw new RuntimeException("Could not initialize API.", ex);
            }
        }

        apis = apiBuilder.build();
    }

    public Map<String, Optional<Boolean>> test(String ip, Set<String> sources, ConfigurationNode sourcesConfigNode, boolean debug) {
        CountDownLatch latch = new CountDownLatch(sources.size());

        ConcurrentMap<String, Optional<Boolean>> retVal = new ConcurrentLinkedHashMap.Builder<String, Optional<Boolean>>().maximumWeightedCapacity(Long.MAX_VALUE).build();

        for (String source : sources) {
            threadPool.submit(() -> {
                API api = apis.get(source);
                if (api == null) {
                    if (debug) {
                        logger.info(source + " has an invalid/missing API.");
                    }
                    latch.countDown();
                    return;
                }

                retVal.put(source, api.getResult(ip, sourcesConfigNode.getNode(source)));
                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }

        return retVal;
    }

    public Optional<Boolean> getResult(String ip, String source, ConfigurationNode sourceConfigNode) {
        API api = apis.get(source);
        if (api == null) {
            return Optional.empty();
        }
        return api.getResult(ip, sourceConfigNode);
    }

    public boolean isVPN(String ip, boolean expensive, long sourceCacheTime, JedisPool redisPool, ConfigurationNode redisConfigNode, Connection rabbitConnection, SQL sql, ConfigurationNode storageConfigNode, SQLType sqlType, Set<String> sources, ConfigurationNode sourcesConfigNode, boolean debug) {
        boolean retVal = ipCache.get(ip, f -> resultExpensive(ip, expensive, sourceCacheTime, redisPool, redisConfigNode, rabbitConnection, sql, storageConfigNode, sqlType, sources, sourcesConfigNode, debug));
        if (debug) {
            logger.info(ip + " cascade value cached. Value: " + retVal);
        }
        return retVal;
    }

    public double consensus(String ip, boolean expensive, long sourceCacheTime, JedisPool redisPool, ConfigurationNode redisConfigNode, Connection rabbitConnection, SQL sql, ConfigurationNode storageConfigNode, SQLType sqlType, Set<String> sources, ConfigurationNode sourcesConfigNode, boolean debug) {
        double retVal = ipConsensusCache.get(ip, f -> consensusExpensive(ip, expensive, sourceCacheTime, redisPool, redisConfigNode, rabbitConnection, sql, storageConfigNode, sqlType, sources, sourcesConfigNode, debug));
        if (debug) {
            logger.info(ip + " consensus value cached. Value: " + retVal);
        }
        return retVal;
    }

    public static void changeThreadCount(int newThreadCount) {
        threadPool = Executors.newWorkStealingPool(newThreadCount);
    }

    public static void changeCacheTime(long cacheDuration, TimeUnit cacheUnit, long sourceDuration, TimeUnit sourceUnit) {
        ipCache = Caffeine.newBuilder().expireAfterAccess(cacheDuration, cacheUnit).expireAfterWrite(sourceDuration, sourceUnit).build();
        ipConsensusCache = Caffeine.newBuilder().expireAfterAccess(cacheDuration, cacheUnit).expireAfterWrite(sourceDuration, sourceUnit).build();
    }

    public static void set(String ip, boolean value, long created, SQL sql, ConfigurationNode storageConfigNode, SQLType sqlType) {
        ipCache.put(ip, value);
        if (sqlType == SQLType.SQLite) {
            SQLite.set(ip, value, created, sql, storageConfigNode);
        }
    }

    public static void set(String ip, double value, long created, SQL sql, ConfigurationNode storageConfigNode, SQLType sqlType) {
        ipConsensusCache.put(ip, value);
        if (sqlType == SQLType.SQLite) {
            SQLite.set(ip, value, created, sql, storageConfigNode);
        }
    }

    public static void delete(String ip, SQL sql, ConfigurationNode storageConfigNode, SQLType sqlType) {
        ipCache.invalidate(ip);
        ipConsensusCache.invalidate(ip);
        if (sqlType == SQLType.SQLite) {
            SQLite.delete(ip, sql, storageConfigNode);
        }
    }

    private boolean resultExpensive(String ip, boolean expensive, long sourceCacheTime, JedisPool redisPool, ConfigurationNode redisConfigNode, Connection rabbitConnection, SQL sql, ConfigurationNode storageConfigNode, SQLType sqlType, Set<String> sources, ConfigurationNode sourcesConfigNode, boolean debug) {
        if (debug) {
            logger.info("Getting cascade for " + ip);
        }

        // Redis
        try {
            Boolean result = Redis.getResult(ip, redisPool, redisConfigNode).get();
            if (result != null) {
                if (debug) {
                    logger.info(ip + " cascade found in Redis. Value: " + result);
                }
                return result;
            }
        } catch (ExecutionException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }

        // SQL
        try {
            DataResult result = null;
            if (sqlType == SQLType.MySQL) {
                result = MySQL.getResult(ip, sql, storageConfigNode, sourceCacheTime).get();
            } else if (sqlType == SQLType.SQLite) {
                result = SQLite.getResult(ip, sql, storageConfigNode, sourceCacheTime).get();
            }

            if (result != null) {
                if (debug) {
                    logger.info(ip + " consensus found in storage. Value: " + result);
                }
                // Update messaging/Redis, force same-thread
                Redis.update(result, sourceCacheTime, redisPool, redisConfigNode).get();
                RabbitMQ.broadcast(result, sourceCacheTime, rabbitConnection).get();
                return result.getValue();
            }
        } catch (ExecutionException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }

        if (!expensive) {
            // Non-expensive call. Return false
            if (debug) {
                logger.info(ip + " cascade fetch call is non-expensive and does not have a cached value. Returning default value of false.");
            }
            return false;
        }

        // API lookup
        boolean retVal = false;

        for (String source : sources) {
            if (!sourceValidationCache.get(source)) {
                if (debug) {
                    logger.info("Skipping " + source + " for " + ip + " cascade due to recently bad/failed check.");
                }
                continue;
            }

            if (debug) {
                logger.info("Trying " + source + " as next cascade source for " + ip + ".");
            }

            API api = apis.get(source);
            if (api == null) {
                if (debug) {
                    logger.info(source + " has an invalid/missing API for " + ip + " cascade.");
                }
                continue;
            }

            Optional<Boolean> result = api.getResult(ip, sourcesConfigNode.getNode(source));
            if (!result.isPresent()) {
                if (debug) {
                    logger.info(source + " returned a bad/failed result for " + ip + " cascade. Skipping source for a while.");
                }
                sourceValidationCache.put(source, Boolean.FALSE);
                continue;
            }

            if (debug) {
                logger.info(source + " returned value \"" + result.get() + "\" for " + ip + " cascade.");
            }

            retVal = result.get();
        }

        if (debug) {
            logger.info(ip + " cascade fetched via defined sources. Value: " + retVal);
        }

        // Update SQL
        DataResult sqlResult = null;
        try {
            if (sqlType == SQLType.MySQL) {
                sqlResult = MySQL.update(sql, storageConfigNode, ip, retVal).get();
            } else if (sqlType == SQLType.SQLite) {
                sqlResult = SQLite.update(sql, storageConfigNode, ip, retVal).get();
            }
        } catch (InterruptedException | ExecutionException ex) {
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }
        if (sqlResult == null) {
            // SQL error, don't continue
            return retVal;
        }

        // Update messaging/Redis, force same-thread
        try {
            Redis.update(sqlResult, sourceCacheTime, redisPool, redisConfigNode).get();
            RabbitMQ.broadcast(sqlResult, sourceCacheTime, rabbitConnection).get();
        } catch (ExecutionException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }
        return retVal;
    }

    private double consensusExpensive(String ip, boolean expensive, long sourceCacheTime, JedisPool redisPool, ConfigurationNode redisConfigNode, Connection rabbitConnection, SQL sql, ConfigurationNode storageConfigNode, SQLType sqlType, Set<String> sources, ConfigurationNode sourcesConfigNode, boolean debug) {
        if (debug) {
            logger.info("Getting consensus for " + ip);
        }

        // Redis
        try {
            Double result = Redis.getConsensus(ip, redisPool, redisConfigNode).get();
            if (result != null) {
                if (debug) {
                    logger.info(ip + " consensus found in Redis. Value: " + result);
                }
                return result;
            }
        } catch (ExecutionException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }

        // SQL
        try {
            ConsensusResult result = null;
            if (sqlType == SQLType.MySQL) {
                result = MySQL.getConsensus(ip, sql, storageConfigNode, sourceCacheTime).get();
            } else if (sqlType == SQLType.SQLite) {
                result = SQLite.getConsensus(ip, sql, storageConfigNode, sourceCacheTime).get();
            }

            if (result != null) {
                if (debug) {
                    logger.info(ip + " consensus found in storage. Value: " + result);
                }
                // Update messaging/Redis, force same-thread
                Redis.update(result, sourceCacheTime, redisPool, redisConfigNode).get();
                RabbitMQ.broadcast(result, sourceCacheTime, rabbitConnection).get();
                return result.getValue();
            }
        } catch (ExecutionException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }

        if (!expensive) {
            // Non-expensive call. Return 0
            if (debug) {
                logger.info(ip + " consensus fetch call is non-expensive and does not have a cached value. Returning default value of 0.");
            }
            return 0.0d;
        }

        // API lookup
        AtomicDouble servicesCount = new AtomicDouble(0.0d);
        AtomicDouble currentValue = new AtomicDouble(0.0d);

        CountDownLatch latch = new CountDownLatch(sources.size());

        for (String source : sources) {
            threadPool.submit(() -> {
                if (!sourceValidationCache.get(source)) {
                    if (debug) {
                        logger.info("Skipping " + source + " for " + ip + " consensus due to recently bad/failed check.");
                    }
                    latch.countDown();
                    return;
                }

                if (debug) {
                    logger.info("Trying " + source + " as next consensus source for " + ip + ".");
                }

                API api = apis.get(source);
                if (api == null) {
                    if (debug) {
                        logger.info(source + " has an invalid/missing API for " + ip + "  consensus.");
                    }
                    latch.countDown();
                    return;
                }

                Optional<Boolean> result = api.getResult(ip, sourcesConfigNode.getNode(source));
                if (!result.isPresent()) {
                    if (debug) {
                        logger.info(source + " returned a bad/failed result for " + ip + " consensus. Skipping source for a while.");
                    }
                    sourceValidationCache.put(source, Boolean.FALSE);
                    latch.countDown();
                    return;
                }

                if (debug) {
                    logger.info(source + " returned value \"" + result.get() + "\" for " + ip + " consensus.");
                }

                servicesCount.getAndAdd(1.0d);
                currentValue.getAndAdd(result.get() ? 1.0d : 0.0d);
                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }

        double result = Double.valueOf(currentValue.get() / servicesCount.get());

        if (debug) {
            logger.info(ip + " consensus fetched via defined sources. Value: " + result);
        }

        // Update SQL
        ConsensusResult sqlResult = null;
        try {
            if (sqlType == SQLType.MySQL) {
                sqlResult = MySQL.update(sql, storageConfigNode, ip, result).get();
            } else if (sqlType == SQLType.SQLite) {
                sqlResult = SQLite.update(sql, storageConfigNode, ip, result).get();
            }
        } catch (InterruptedException | ExecutionException ex) {
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }
        if (sqlResult == null) {
            // SQL error, don't continue
            return result;
        }

        // Update messaging/Redis, force same-thread
        try {
            Redis.update(sqlResult, sourceCacheTime, redisPool, redisConfigNode).get();
            RabbitMQ.broadcast(sqlResult, sourceCacheTime, rabbitConnection).get();
        } catch (ExecutionException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }
        return result;
    }
}

package me.egg82.antivpn.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AtomicDouble;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.apis.API;
import me.egg82.antivpn.core.ConsensusResult;
import me.egg82.antivpn.core.DataResult;
import me.egg82.antivpn.enums.SQLType;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.sql.MySQL;
import me.egg82.antivpn.sql.SQLite;
import me.egg82.antivpn.utils.ConfigUtil;
import ninja.egg82.reflect.PackageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternalAPI {
    private static final Logger logger = LoggerFactory.getLogger(InternalAPI.class);

    private static Cache<String, Boolean> ipCache = Caffeine.newBuilder().expireAfterAccess(1L, TimeUnit.MINUTES).expireAfterWrite(1L, TimeUnit.HOURS).build();
    private static Cache<String, Double> ipConsensusCache = Caffeine.newBuilder().expireAfterAccess(1L, TimeUnit.MINUTES).expireAfterWrite(1L, TimeUnit.HOURS).build();
    private static LoadingCache<String, Boolean> sourceValidationCache = Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES).build(k -> Boolean.TRUE);

    private final Object ipCacheLock = new Object();
    private final Object ipConsensusCacheLock = new Object();

    private static ImmutableMap<String, API> apis = ImmutableMap.of();

    public static void initialize(boolean debug) {
        ImmutableMap.Builder<String, API> apiBuilder = ImmutableMap.builder();

        List<Class<API>> list = PackageFilter.getClasses(API.class, "me.egg82.antivpn.apis", false, false, false);
        if (debug) {
            logger.info("Initializing " + list.size() + " APIs..");
        }
        for (Class<API> clazz : list) {
            if (debug) {
                logger.info("Initializing API " + clazz.getName());
            }

            try {
                API api = clazz.newInstance();
                apiBuilder.put(api.getName(), api);
            } catch (InstantiationException | IllegalAccessException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        apis = apiBuilder.build();
    }

    private static ExecutorService threadPool = Executors.newWorkStealingPool(4);

    public InternalAPI() { }

    public static Optional<API> getAPI(String name) { return Optional.ofNullable(apis.getOrDefault(name, null)); }

    public Map<String, Optional<Boolean>> testAllSources(String ip) throws APIException {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get cached config.");
        }

        CountDownLatch latch = new CountDownLatch(cachedConfig.get().getSources().size());

        ConcurrentMap<String, Optional<Boolean>> retVal = new ConcurrentLinkedHashMap.Builder<String, Optional<Boolean>>().maximumWeightedCapacity(Long.MAX_VALUE).build();

        for (String source : cachedConfig.get().getSources()) {
            threadPool.submit(() -> {
                Optional<API> api = getAPI(source);
                if (!api.isPresent()) {
                    if (ConfigUtil.getDebugOrFalse()) {
                        logger.info(source + " has an invalid/missing API.");
                    }
                    latch.countDown();
                    return;
                }

                try {
                    retVal.put(source, Optional.of(api.get().getResult(ip)));
                } catch (APIException ex) {
                    if (ex.isHard()) {
                        logger.error(ex.getMessage(), ex);
                    }
                    retVal.put(source, Optional.empty());
                }
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

    public boolean getSourceResult(String ip, String source) throws APIException {
        Optional<API> api = getAPI(source);
        if (!api.isPresent()) {
            throw new APIException(true, "Source does not exist.");
        }
        return api.get().getResult(ip);
    }

    public boolean cascade(String ip, boolean expensive) throws APIException {
        Optional<Boolean> retVal = Optional.ofNullable(ipCache.getIfPresent(ip));
        if (!retVal.isPresent()) {
            synchronized (ipCacheLock) {
                retVal = Optional.ofNullable(ipCache.getIfPresent(ip));
                if (!retVal.isPresent()) {
                    retVal = Optional.of(resultExpensive(ip, expensive));
                    ipCache.put(ip, retVal.get());
                }
            }
        }
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info(ip + " cascade value cached. Value: " + retVal.get());
        }
        return retVal.get();
    }

    public double consensus(String ip, boolean expensive) throws APIException {
        Optional<Double> retVal = Optional.ofNullable(ipConsensusCache.getIfPresent(ip));
        if (!retVal.isPresent()) {
            synchronized (ipConsensusCacheLock) {
                retVal = Optional.ofNullable(ipConsensusCache.getIfPresent(ip));
                if (!retVal.isPresent()) {
                    retVal = Optional.of(consensusExpensive(ip, expensive));
                    ipConsensusCache.put(ip, retVal.get());
                }
            }
        }
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info(ip + " consensus value cached. Value: " + retVal.get());
        }
        return retVal.get();
    }

    public static void changeThreadCount(int newThreadCount) {
        threadPool = Executors.newWorkStealingPool(newThreadCount);
    }

    public static void changeCacheTime(long duration, TimeUnit unit) {
        ipCache = Caffeine.newBuilder().expireAfterAccess(duration, unit).expireAfterWrite(1L, TimeUnit.HOURS).build();
        ipConsensusCache = Caffeine.newBuilder().expireAfterAccess(duration, unit).expireAfterWrite(1L, TimeUnit.HOURS).build();
    }

    public static void set(String ip, boolean value, long created) throws APIException {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get cached config.");
        }

        ipCache.put(ip, value);
        if (cachedConfig.get().getSQLType() == SQLType.SQLite) {
            try {
                SQLite.set(ip, value, created);
            } catch (SQLException ex) {
                throw new APIException(true, ex);
            }
        }
    }

    public static void set(String ip, double value, long created) throws APIException {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get cached config.");
        }

        ipConsensusCache.put(ip, value);
        if (cachedConfig.get().getSQLType() == SQLType.SQLite) {
            try {
                SQLite.set(ip, value, created);
            } catch (SQLException ex) {
                throw new APIException(true, ex);
            }
        }
    }

    public static void delete(String ip) throws APIException {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get cached config.");
        }

        ipCache.invalidate(ip);
        ipConsensusCache.invalidate(ip);
        if (cachedConfig.get().getSQLType() == SQLType.SQLite) {
            try {
                SQLite.delete(ip);
            } catch (SQLException ex) {
                throw new APIException(true, ex);
            }
        }
    }

    private boolean resultExpensive(String ip, boolean expensive) throws APIException {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get cached config.");
        }

        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Getting cascade for " + ip);
        }

        // Redis
        Optional<Boolean> redisResult = Redis.getResult(ip);
        if (redisResult.isPresent()) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(ip + " cascade found in Redis. Value: " + redisResult.get());
            }
            return redisResult.get();
        }

        // SQL
        try {
            Optional<DataResult> result = Optional.empty();
            if (cachedConfig.get().getSQLType() == SQLType.MySQL) {
                result = MySQL.getResult(ip);
            } else if (cachedConfig.get().getSQLType() == SQLType.SQLite) {
                result = SQLite.getResult(ip);
            }

            if (result.isPresent()) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(ip + " cascade found in storage. Value: " + result.get());
                }
                // Update messaging/Redis
                Redis.update(result.get());
                RabbitMQ.broadcast(result.get());
                return result.get().getValue();
            }
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            throw new APIException(true, ex);
        }

        if (!expensive) {
            // Non-expensive call. Return false
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(ip + " cascade fetch call is non-expensive and does not have a cached value. Returning default value of false.");
            }
            return false;
        }

        // API lookup
        boolean retVal = false;

        for (String source : cachedConfig.get().getSources()) {
            if (!sourceValidationCache.get(source)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Skipping " + source + " for " + ip + " cascade due to recently bad/failed check.");
                }
                continue;
            }

            if (ConfigUtil.getDebugOrFalse()) {
                logger.info("Trying " + source + " as next cascade source for " + ip + ".");
            }

            Optional<API> api = getAPI(source);
            if (!api.isPresent()) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(source + " has an invalid/missing API for " + ip + " cascade.");
                }
                continue;
            }

            boolean result;
            try {
                result = api.get().getResult(ip);
            } catch (APIException ex) {
                if (ex.isHard()) {
                    throw ex;
                }

                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(source + " returned a bad/failed result for " + ip + " cascade. Skipping source for a while.");
                }
                sourceValidationCache.put(source, Boolean.FALSE);
                continue;
            }

            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(source + " returned value \"" + result + "\" for " + ip + " cascade.");
            }

            retVal = result;
            break;
        }

        if (ConfigUtil.getDebugOrFalse()) {
            logger.info(ip + " cascade fetched via defined sources. Value: " + retVal);
        }

        // Update SQL
        DataResult sqlResult = null;
        try {
            if (cachedConfig.get().getSQLType() == SQLType.MySQL) {
                sqlResult = MySQL.update(ip, retVal);
            } else if (cachedConfig.get().getSQLType() == SQLType.SQLite) {
                sqlResult = SQLite.update(ip, retVal);
            }
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            throw new APIException(true, ex);
        }

        // Update messaging/Redis
        Redis.update(sqlResult);
        RabbitMQ.broadcast(sqlResult);

        return retVal;
    }

    private double consensusExpensive(String ip, boolean expensive) throws APIException {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get cached config.");
        }

        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Getting consensus for " + ip);
        }

        // Redis
        Optional<Double> redisResult = Redis.getConsensus(ip);
        if (redisResult.isPresent()) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(ip + " consensus found in Redis. Value: " + redisResult.get());
            }
            return redisResult.get();
        }

        // SQL
        try {
            Optional<ConsensusResult> result = Optional.empty();
            if (cachedConfig.get().getSQLType() == SQLType.MySQL) {
                result = MySQL.getConsensus(ip);
            } else if (cachedConfig.get().getSQLType() == SQLType.SQLite) {
                result = SQLite.getConsensus(ip);
            }

            if (result.isPresent()) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(ip + " consensus found in storage. Value: " + result.get());
                }
                // Update messaging/Redis
                Redis.update(result.get());
                RabbitMQ.broadcast(result.get());
                return result.get().getValue();
            }
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            throw new APIException(true, ex);
        }

        if (!expensive) {
            // Non-expensive call. Return 0
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(ip + " consensus fetch call is non-expensive and does not have a cached value. Returning default value of 0.");
            }
            return 0.0d;
        }

        // API lookup
        AtomicDouble servicesCount = new AtomicDouble(0.0d);
        AtomicDouble currentValue = new AtomicDouble(0.0d);

        CountDownLatch latch = new CountDownLatch(cachedConfig.get().getSources().size());

        for (String source : cachedConfig.get().getSources()) {
            threadPool.submit(() -> {
                if (!sourceValidationCache.get(source)) {
                    if (ConfigUtil.getDebugOrFalse()) {
                        logger.info("Skipping " + source + " for " + ip + " consensus due to recently bad/failed check.");
                    }
                    latch.countDown();
                    return;
                }

                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Trying " + source + " as next consensus source for " + ip + ".");
                }

                Optional<API> api = getAPI(source);
                if (!api.isPresent()) {
                    if (ConfigUtil.getDebugOrFalse()) {
                        logger.info(source + " has an invalid/missing API for " + ip + "  consensus.");
                    }
                    latch.countDown();
                    return;
                }

                boolean result;
                try {
                    result = api.get().getResult(ip);
                } catch (APIException ex) {
                    if (ex.isHard()) {
                        logger.error(ex.getMessage(), ex);
                    }

                    if (ConfigUtil.getDebugOrFalse()) {
                        logger.info(source + " returned a bad/failed result for " + ip + " consensus. Skipping source for a while.");
                    }
                    sourceValidationCache.put(source, Boolean.FALSE);
                    latch.countDown();
                    return;
                }

                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(source + " returned value \"" + result + "\" for " + ip + " consensus.");
                }

                servicesCount.getAndAdd(1.0d);
                currentValue.getAndAdd(result ? 1.0d : 0.0d);
                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }

        double result = currentValue.get() / servicesCount.get();
        if (Double.isNaN(result)) {
            throw new APIException(true, "Consensus had no valid/usable sources. (NaN result)");
        }
        if (Double.isInfinite(result)) {
            throw new APIException(true, "Consensus had an infinite result. (result with no valid sources)");
        }

        if (ConfigUtil.getDebugOrFalse()) {
            logger.info(ip + " consensus fetched via defined sources. Value: " + result);
        }

        // Update SQL
        ConsensusResult sqlResult = null;
        try {
            if (cachedConfig.get().getSQLType() == SQLType.MySQL) {
                sqlResult = MySQL.update(ip, result);
            } else if (cachedConfig.get().getSQLType() == SQLType.SQLite) {
                sqlResult = SQLite.update(ip, result);
            }
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            throw new APIException(true, ex);
        }

        // Update messaging/Redis
        Redis.update(sqlResult);
        RabbitMQ.broadcast(sqlResult);

        return result;
    }
}

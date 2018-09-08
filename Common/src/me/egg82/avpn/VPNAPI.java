package me.egg82.avpn;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AtomicDouble;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import me.egg82.avpn.apis.IFetchAPI;
import me.egg82.avpn.core.ConsensusResultEventArgs;
import me.egg82.avpn.core.ResultEventArgs;
import me.egg82.avpn.debug.IDebugPrinter;
import me.egg82.avpn.registries.ConsensusRegistry;
import me.egg82.avpn.registries.IPRegistry;
import me.egg82.avpn.registries.InvalidRegistry;
import me.egg82.avpn.sql.mysql.SelectConsensusMySQLCommand;
import me.egg82.avpn.sql.mysql.SelectResultMySQLCommand;
import me.egg82.avpn.sql.sqlite.SelectConsensusSQLiteCommand;
import me.egg82.avpn.sql.sqlite.SelectResultSQLiteCommand;
import me.egg82.avpn.utils.IPCacheUtil;
import me.egg82.avpn.utils.RedisUtil;
import me.egg82.avpn.utils.ValidationUtil;
import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.enums.BaseSQLType;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.registries.IExpiringRegistry;
import ninja.egg82.patterns.registries.IRegistry;
import ninja.egg82.patterns.registries.Registry;
import ninja.egg82.sql.ISQL;
import ninja.egg82.utils.ReflectUtil;
import ninja.egg82.utils.ThreadUtil;
import redis.clients.jedis.Jedis;

public class VPNAPI {
    // vars
    private static VPNAPI api = new VPNAPI();

    private IRegistry<String, IFetchAPI> apis = new Registry<String, IFetchAPI>(String.class, IFetchAPI.class);

    // constructor
    public VPNAPI() {
        List<Class<IFetchAPI>> list = ReflectUtil.getClasses(IFetchAPI.class, "me.egg82.avpn.apis", false, false, false);
        for (Class<IFetchAPI> clazz : list) {
            try {
                IFetchAPI api = clazz.newInstance();
                apis.setRegister(api.getName(), api);
            } catch (Exception ex) {
                IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
                if (handler != null) {
                    handler.sendException(ex);
                }
                throw new RuntimeException("Cannot initialize API service.", ex);
            }
        }
    }

    // public
    public static VPNAPI getInstance() {
        return api;
    }

    public ImmutableMap<String, Optional<Boolean>> test(String ip) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            return ImmutableMap.of();
        }

        CountDownLatch latch = new CountDownLatch(Config.sources.size());

        ConcurrentMap<String, Optional<Boolean>> retVal = new ConcurrentLinkedHashMap.Builder<String, Optional<Boolean>>().build();

        for (String source : Config.sources) {
            ThreadUtil.submit(new Runnable() {
                public void run() {
                    IFetchAPI api = apis.getRegister(source);
                    if (api == null) {
                        if (Config.debug) {
                            ServiceLocator.getService(IDebugPrinter.class).printInfo(source + " has a bad API.");
                        }
                        latch.countDown();
                        return;
                    }

                    retVal.put(source, api.getResult(ip));
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (Exception ex) {
            IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
            if (handler != null) {
                handler.sendException(ex);
            }
            ex.printStackTrace();
        }

        return ImmutableMap.copyOf(retVal);
    }

    public double consensus(String ip) {
        return consensus(ip, true);
    }
    public double consensus(String ip, boolean expensive) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            return 0.0d;
        }

        if (Config.debug) {
            ServiceLocator.getService(IDebugPrinter.class).printInfo("Getting consensus for IP " + ip);
        }

        // Internal cache - use first
        IExpiringRegistry<String, Double> consensusRegistry = ServiceLocator.getService(ConsensusRegistry.class);
        Double doub = consensusRegistry.getRegister(ip);
        if (doub != null) {
            if (Config.debug) {
                ServiceLocator.getService(IDebugPrinter.class).printInfo(ip + " consensus found in local cache. Value: " + doub.toString());
            }
            return doub.doubleValue();
        }

        // Redis - use BEFORE SQL
        try (Jedis redis = RedisUtil.getRedis()) {
            if (redis != null) {
                String key = "avpn:consensus:" + ip;

                // Grab IP info
                String data = redis.get(key);
                if (data != null) {
                    doub = Double.valueOf(data);
                }
            }
        }

        if (doub != null) {
            // Redis returned some data. Cache the result
            consensusRegistry.setRegister(ip, doub);
            if (Config.debug) {
                ServiceLocator.getService(IDebugPrinter.class).printInfo(ip + " consensus found in Redis. Value: " + doub.toString());
            }
            return doub.doubleValue();
        }

        // SQL - use BEFORE lookup
        AtomicReference<ConsensusResultEventArgs> retVal = new AtomicReference<ConsensusResultEventArgs>(null);
        CountDownLatch latch = new CountDownLatch(1);

        BiConsumer<Object, ConsensusResultEventArgs> sqlData = (s, e) -> {
            retVal.set(e);
            latch.countDown();

            ISQL sql = ServiceLocator.getService(ISQL.class);
            if (sql.getType() == BaseSQLType.MySQL) {
                SelectConsensusMySQLCommand c = (SelectConsensusMySQLCommand) s;
                c.onData().detatchAll();
            } else if (sql.getType() == BaseSQLType.SQLite) {
                SelectConsensusSQLiteCommand c = (SelectConsensusSQLiteCommand) s;
                c.onData().detatchAll();
            }
        };

        ISQL sql = ServiceLocator.getService(ISQL.class);
        if (sql.getType() == BaseSQLType.MySQL) {
            SelectConsensusMySQLCommand command = new SelectConsensusMySQLCommand(ip);
            command.onData().attach(sqlData);
            command.start();
        } else if (sql.getType() == BaseSQLType.SQLite) {
            SelectConsensusSQLiteCommand command = new SelectConsensusSQLiteCommand(ip);
            command.onData().attach(sqlData);
            command.start();
        }

        try {
            latch.await();
        } catch (Exception ex) {
            IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
            if (handler != null) {
                handler.sendException(ex);
            }
            ex.printStackTrace();
        }

        if (retVal.get() != null && retVal.get().getIp() != null && retVal.get().getValue() != null) {
            // Set Redis, if available
            try (Jedis redis = RedisUtil.getRedis()) {
                if (redis != null) {
                    String key = "avpn:consensus:" + ip;
                    int offset = (int) Math.floorDiv((Config.sourceCacheTime + retVal.get().getCreated()) - System.currentTimeMillis(), 1000L);
                    if (offset > 0) {
                        redis.setex(key, offset, retVal.get().getValue().toString());
                    } else {
                        redis.del(key);
                    }
                }
            }

            doub = retVal.get().getValue();
            // Cache the result
            consensusRegistry.setRegister(ip, doub);

            if (Config.debug) {
                ServiceLocator.getService(IDebugPrinter.class).printInfo(ip + " consensus found in SQL. Value: " + doub.toString());
            }
            return doub.doubleValue();
        }

        if (!expensive) {
            // Non-expensive call. Return 0, but don't cache this result
            return 0.0d;
        }

        AtomicDouble servicesCount = new AtomicDouble(0.0d);
        AtomicDouble currentValue = new AtomicDouble(0.0d);

        CountDownLatch latch2 = new CountDownLatch(Config.sources.size());

        // Lookup via APIs - use as last resort
        IRegistry<String, Boolean> invalidRegistry = ServiceLocator.getService(InvalidRegistry.class);
        for (String source : Config.sources) {
            ThreadUtil.submit(new Runnable() {
                public void run() {
                    if (invalidRegistry.hasRegister(source)) {
                        if (Config.debug) {
                            ServiceLocator.getService(IDebugPrinter.class).printInfo("Skipping " + source + " for " + ip + " consensus due to recently bad/failed check.");
                        }
                        latch2.countDown();
                        return;
                    }

                    if (Config.debug) {
                        ServiceLocator.getService(IDebugPrinter.class).printInfo("Trying " + source + " as next consensus source for " + ip + ".");
                    }

                    IFetchAPI api = apis.getRegister(source);
                    if (api == null) {
                        if (Config.debug) {
                            ServiceLocator.getService(IDebugPrinter.class).printInfo(source + " has a bad API.");
                        }
                        latch2.countDown();
                        return;
                    }

                    Boolean bool = api.getResult(ip).orElse(null);
                    if (bool == null) {
                        if (Config.debug) {
                            ServiceLocator.getService(IDebugPrinter.class).printInfo(source + " returned a bad/failed result for " + ip + " consensus. Skipping source for a while.");
                        }
                        invalidRegistry.setRegister(source, Boolean.TRUE);
                        latch2.countDown();
                        return;
                    }

                    if (Config.debug) {
                        ServiceLocator.getService(IDebugPrinter.class).printInfo(source + " returned value \"" + bool.toString() + "\" for " + ip + " consensus.");
                    }
                    servicesCount.getAndAdd(1.0d);
                    currentValue.getAndAdd((bool.booleanValue()) ? 1.0d : 0.0d);
                    latch2.countDown();
                }
            });
        }

        try {
            latch2.await();
        } catch (Exception ex) {
            IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
            if (handler != null) {
                handler.sendException(ex);
            }
            ex.printStackTrace();
        }

        doub = Double.valueOf(currentValue.get() / servicesCount.get());

        // Add the result
        IPCacheUtil.addInfo(ip, doub.doubleValue());

        if (Config.debug) {
            ServiceLocator.getService(IDebugPrinter.class).printInfo(ip + " consensus fetched via defined sources. Value: " + doub.toString());
        }
        return doub.doubleValue();
    }

    public Optional<Boolean> getResult(String ip, String sourceName) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (sourceName == null) {
            throw new IllegalArgumentException("sourceName cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            return Optional.empty();
        }

        IFetchAPI api = apis.getRegister(sourceName);
        if (api == null) {
            return Optional.empty();
        }

        return api.getResult(ip);
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
            ServiceLocator.getService(IDebugPrinter.class).printInfo("Checking IP " + ip);
        }

        // Internal cache - use first
        IExpiringRegistry<String, Boolean> ipRegistry = ServiceLocator.getService(IPRegistry.class);
        Boolean bool = ipRegistry.getRegister(ip);
        if (bool != null) {
            if (Config.debug) {
                ServiceLocator.getService(IDebugPrinter.class).printInfo(ip + " found in local cache. Value: " + bool.toString());
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
                ServiceLocator.getService(IDebugPrinter.class).printInfo(ip + " found in Redis. Value: " + bool.toString());
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
            IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
            if (handler != null) {
                handler.sendException(ex);
            }
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
                ServiceLocator.getService(IDebugPrinter.class).printInfo(ip + " found in SQL. Value: " + bool.toString());
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
                    ServiceLocator.getService(IDebugPrinter.class).printInfo("Skipping " + source + " for " + ip + " due to recently bad/failed check.");
                }
                continue;
            }

            if (Config.debug) {
                ServiceLocator.getService(IDebugPrinter.class).printInfo("Trying " + source + " as next source for " + ip + ".");
            }

            IFetchAPI api = apis.getRegister(source);
            if (api != null) {
                bool = api.getResult(ip).orElse(null);
            }

            if (bool != null) {
                break;
            }

            if (Config.debug) {
                ServiceLocator.getService(IDebugPrinter.class).printInfo(source + " returned a bad/failed result for " + ip + ". Skipping source for a while.");
            }
            invalidRegistry.setRegister(source, Boolean.TRUE);
        }

        if (bool == null) {
            // Something went wrong. return false, but don't cache this
            if (Config.debug) {
                ServiceLocator.getService(IDebugPrinter.class).printInfo(ip + " Was not able to be fetched. Returning \"false\" as default value.");
            }
            return false;
        }

        // Add the result
        IPCacheUtil.addInfo(ip, bool.booleanValue());

        if (Config.debug) {
            ServiceLocator.getService(IDebugPrinter.class).printInfo(ip + " fetched via defined sources. Value: " + bool.toString());
        }
        return bool.booleanValue();
    }

    // private

}

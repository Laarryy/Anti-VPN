package me.egg82.avpn.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import me.egg82.avpn.Config;
import me.egg82.avpn.core.UpdateConsensusEventArgs;
import me.egg82.avpn.core.UpdateEventArgs;
import me.egg82.avpn.registries.ConsensusRegistry;
import me.egg82.avpn.registries.IPRegistry;
import me.egg82.avpn.sql.mysql.UpdateConsensusMySQLCommand;
import me.egg82.avpn.sql.mysql.UpdateDataMySQLCommand;
import me.egg82.avpn.sql.sqlite.UpdateConsensusSQLiteCommand;
import me.egg82.avpn.sql.sqlite.UpdateDataSQLiteCommand;
import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.enums.BaseSQLType;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.registries.IExpiringRegistry;
import ninja.egg82.plugin.messaging.IMessageHandler;
import ninja.egg82.sql.ISQL;
import ninja.egg82.utils.ThreadUtil;
import redis.clients.jedis.Jedis;

public class IPCacheUtil {
    // vars

    // constructor
    public IPCacheUtil() {

    }

    // public
    public static void addToCache(String ip, double value, long created, boolean doInsert) {
        if (!ValidationUtil.isValidIp(ip)) {
            return;
        }
        IExpiringRegistry<String, Double> consensusRegistry = ServiceLocator.getService(ConsensusRegistry.class);

        // Add to IP->UUIDs cache
        if (!consensusRegistry.hasRegister(ip)) {
            // Don't want to trigger exceptions from getTimeRemaining
            return;
        }

        long expirationTime = consensusRegistry.getTimeRemaining(ip);
        // Add the new value (replace if already present)
        consensusRegistry.setRegister(ip, Double.valueOf(value));
        // Set expiration back- we don't want to constantly re-set expiration times or
        // we'll have a bad time
        consensusRegistry.setRegisterExpiration(ip, expirationTime, TimeUnit.MILLISECONDS);

        if (doInsert) {
            ISQL sql = ServiceLocator.getService(ISQL.class);
            // Update data in local tables if SQLite is used
            if (sql != null && sql.getType() == BaseSQLType.SQLite) {
                new UpdateConsensusSQLiteCommand(ip, value, created).start();
            }
        }
    }

    public static void addToCache(String ip, boolean value, long created, boolean doInsert) {
        if (!ValidationUtil.isValidIp(ip)) {
            return;
        }
        IExpiringRegistry<String, Boolean> ipRegistry = ServiceLocator.getService(IPRegistry.class);

        // Add to IP->UUIDs cache
        if (!ipRegistry.hasRegister(ip)) {
            // Don't want to trigger exceptions from getTimeRemaining
            return;
        }

        long expirationTime = ipRegistry.getTimeRemaining(ip);
        // Add the new value (replace if already present)
        ipRegistry.setRegister(ip, Boolean.valueOf(value));
        // Set expiration back- we don't want to constantly re-set expiration times or
        // we'll have a bad time
        ipRegistry.setRegisterExpiration(ip, expirationTime, TimeUnit.MILLISECONDS);

        if (doInsert) {
            ISQL sql = ServiceLocator.getService(ISQL.class);
            // Update data in local tables if SQLite is used
            if (sql != null && sql.getType() == BaseSQLType.SQLite) {
                new UpdateDataSQLiteCommand(ip, value, created).start();
            }
        }
    }

    public static void addInfo(String ip, double value) {
        if (!ValidationUtil.isValidIp(ip)) {
            return;
        }

        // Preemptively add to internal cache to hopefully avoid race conditions. We'll
        // update it again later
        // Since current time is only being used for expirations this is relatively safe
        // to do
        // This wouldn't work for other plugins that rely on known "good" times
        IExpiringRegistry<String, Double> consensusRegistry = ServiceLocator.getService(ConsensusRegistry.class);
        consensusRegistry.setRegister(ip, Double.valueOf(value));

        // Preemptively add to Redis to hopefully avoid race conditions. We'll update it
        // again later
        try (Jedis redis = RedisUtil.getRedis()) {
            if (redis != null) {
                String key = "avpn:consensus:" + ip;
                String v = String.valueOf(value);
                int offset = (int) Math.floorDiv((Config.sourceCacheTime + System.currentTimeMillis()) - System.currentTimeMillis(), 1000L);
                if (offset > 0) {
                    // Sadly although "SETNX" exists "SETNXEX" (or similar) does not. We get to
                    // round-trip twice, but it's still faster than SQL
                    if (!redis.exists(key).booleanValue()) {
                        redis.setex(key, offset, v);
                    }
                } else {
                    redis.del(key);
                }
            }
        }

        // Do work in new thread. This is ONLY safe from race conditions due to the fact
        // that UpdateConsensus SQL commands run a non-parallel insert query
        // Meaning even if a race condition were to occur, a SQL lookup would be used
        // and the lookup would be blocked until the insert operation completed
        // This might, in rare cases, cause some extra lag with plugins that get data
        // via the main thread, but would guarantee player logins don't cause the server
        // to lag
        ThreadUtil.submit(new Runnable() {
            public void run() {
                // Add to SQL and get created/updated data back
                AtomicReference<UpdateConsensusEventArgs> retVal = new AtomicReference<UpdateConsensusEventArgs>(null);
                CountDownLatch latch = new CountDownLatch(1);

                BiConsumer<Object, UpdateConsensusEventArgs> sqlData = (s, e) -> {
                    retVal.set(e);

                    ISQL sql = ServiceLocator.getService(ISQL.class);
                    if (sql.getType() == BaseSQLType.MySQL) {
                        UpdateConsensusMySQLCommand c = (UpdateConsensusMySQLCommand) s;
                        c.onUpdated().detatchAll();
                    } else if (sql.getType() == BaseSQLType.SQLite) {
                        UpdateConsensusSQLiteCommand c = (UpdateConsensusSQLiteCommand) s;
                        c.onUpdated().detatchAll();
                    }

                    latch.countDown();
                };

                ISQL sql = ServiceLocator.getService(ISQL.class);
                if (sql.getType() == BaseSQLType.MySQL) {
                    UpdateConsensusMySQLCommand command = new UpdateConsensusMySQLCommand(ip, value);
                    command.onUpdated().attach(sqlData);
                    command.start();
                } else if (sql.getType() == BaseSQLType.SQLite) {
                    UpdateConsensusSQLiteCommand command = new UpdateConsensusSQLiteCommand(ip, value);
                    command.onUpdated().attach(sqlData);
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

                if (retVal.get() == null || retVal.get().getIp() == null) {
                    // Error occurred during SQL functions. We'll skip adding incomplete data
                    // because that seems like a bad idea
                    return;
                }

                // Add to internal cache, if available
                addToCache(ip, value, retVal.get().getCreated(), false);

                // Add to Redis and update other servers, if available
                try (Jedis redis = RedisUtil.getRedis()) {
                    if (redis != null) {
                        String key = "avpn:consensus:" + ip;
                        String v = String.valueOf(value);
                        int offset = (int) Math.floorDiv((Config.sourceCacheTime + retVal.get().getCreated()) - System.currentTimeMillis(), 1000L);
                        if (offset > 0) {
                            redis.setex(key, offset, v);
                        } else {
                            redis.del(key);
                        }

                        redis.publish("avpn-consensus", ip + "," + v + "," + retVal.get().getCreated());
                    }
                }

                // Update other servers through Rabbit, if available
                if (ServiceLocator.hasService(IMessageHandler.class)) {
                    IPChannelUtil.broadcastInfo(ip, value, retVal.get().getCreated());
                }
            }
        });
    }

    public static void addInfo(String ip, boolean value) {
        if (!ValidationUtil.isValidIp(ip)) {
            return;
        }

        // Preemptively add to internal cache to hopefully avoid race conditions. We'll
        // update it again later
        // Since current time is only being used for expirations this is relatively safe
        // to do
        // This wouldn't work for other plugins that rely on known "good" times
        IExpiringRegistry<String, Boolean> ipRegistry = ServiceLocator.getService(IPRegistry.class);
        ipRegistry.setRegister(ip, Boolean.valueOf(value));

        // Preemptively add to Redis to hopefully avoid race conditions. We'll update it
        // again later
        try (Jedis redis = RedisUtil.getRedis()) {
            if (redis != null) {
                String key = "avpn:" + ip;
                String v = String.valueOf(value);
                int offset = (int) Math.floorDiv((Config.sourceCacheTime + System.currentTimeMillis()) - System.currentTimeMillis(), 1000L);
                if (offset > 0) {
                    // Sadly although "SETNX" exists "SETNXEX" (or similar) does not. We get to
                    // round-trip twice, but it's still faster than SQL
                    if (!redis.exists(key).booleanValue()) {
                        redis.setex(key, offset, v);
                    }
                } else {
                    redis.del(key);
                }
            }
        }

        // Do work in new thread. This is ONLY safe from race conditions due to the fact
        // that UpdateData SQL commands run a non-parallel insert query
        // Meaning even if a race condition were to occur, a SQL lookup would be used
        // and the lookup would be blocked until the insert operation completed
        // This might, in rare cases, cause some extra lag with plugins that get data
        // via the main thread, but would guarantee player logins don't cause the server
        // to lag
        ThreadUtil.submit(new Runnable() {
            public void run() {
                // Add to SQL and get created/updated data back
                AtomicReference<UpdateEventArgs> retVal = new AtomicReference<UpdateEventArgs>(null);
                CountDownLatch latch = new CountDownLatch(1);

                BiConsumer<Object, UpdateEventArgs> sqlData = (s, e) -> {
                    retVal.set(e);

                    ISQL sql = ServiceLocator.getService(ISQL.class);
                    if (sql.getType() == BaseSQLType.MySQL) {
                        UpdateDataMySQLCommand c = (UpdateDataMySQLCommand) s;
                        c.onUpdated().detatchAll();
                    } else if (sql.getType() == BaseSQLType.SQLite) {
                        UpdateDataSQLiteCommand c = (UpdateDataSQLiteCommand) s;
                        c.onUpdated().detatchAll();
                    }

                    latch.countDown();
                };

                ISQL sql = ServiceLocator.getService(ISQL.class);
                if (sql.getType() == BaseSQLType.MySQL) {
                    UpdateDataMySQLCommand command = new UpdateDataMySQLCommand(ip, value);
                    command.onUpdated().attach(sqlData);
                    command.start();
                } else if (sql.getType() == BaseSQLType.SQLite) {
                    UpdateDataSQLiteCommand command = new UpdateDataSQLiteCommand(ip, value);
                    command.onUpdated().attach(sqlData);
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

                if (retVal.get() == null || retVal.get().getIp() == null) {
                    // Error occurred during SQL functions. We'll skip adding incomplete data
                    // because that seems like a bad idea
                    return;
                }

                // Add to internal cache, if available
                addToCache(ip, value, retVal.get().getCreated(), false);

                // Add to Redis and update other servers, if available
                try (Jedis redis = RedisUtil.getRedis()) {
                    if (redis != null) {
                        String key = "avpn:" + ip;
                        String v = String.valueOf(value);
                        int offset = (int) Math.floorDiv((Config.sourceCacheTime + retVal.get().getCreated()) - System.currentTimeMillis(), 1000L);
                        if (offset > 0) {
                            redis.setex(key, offset, v);
                        } else {
                            redis.del(key);
                        }

                        redis.publish("avpn", ip + "," + v + "," + retVal.get().getCreated());
                    }
                }

                // Update other servers through Rabbit, if available
                if (ServiceLocator.hasService(IMessageHandler.class)) {
                    IPChannelUtil.broadcastInfo(ip, value, retVal.get().getCreated());
                }
            }
        });
    }

    // private

}

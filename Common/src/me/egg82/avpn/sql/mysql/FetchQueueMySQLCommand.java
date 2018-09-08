package me.egg82.avpn.sql.mysql;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.BiConsumer;

import me.egg82.avpn.Config;
import me.egg82.avpn.utils.IPCacheUtil;
import me.egg82.avpn.utils.RedisUtil;
import me.egg82.avpn.utils.ValidationUtil;
import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.enums.BaseSQLType;
import ninja.egg82.events.CompleteEventArgs;
import ninja.egg82.events.SQLEventArgs;
import ninja.egg82.patterns.Command;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.sql.ISQL;
import redis.clients.jedis.Jedis;

public class FetchQueueMySQLCommand extends Command {
    // vars
    private ISQL sql = ServiceLocator.getService(ISQL.class);

    private UUID fetchQuery = null;
    private UUID consensusFetchQuery = null;
    private UUID queueQuery = null;
    private UUID consensusQueueQuery = null;
    private UUID finalQuery1 = null;
    private UUID finalQuery2 = null;

    private BiConsumer<Object, SQLEventArgs> sqlError = (s, e) -> onSQLError(e);
    private BiConsumer<Object, SQLEventArgs> sqlData = (s, e) -> onSQLData(e);

    // constructor
    public FetchQueueMySQLCommand() {
        super();

        sql.onError().attach(sqlError);
        sql.onData().attach(sqlData);
    }

    // public

    // private
    protected void onExecute(long elapsedMilliseconds) {
        if (sql.getType() == BaseSQLType.SQLite) {
            sql.onError().detatch(sqlError);
            sql.onData().detatch(sqlError);

            onComplete().invoke(this, CompleteEventArgs.EMPTY);
            return;
        }

        fetchQuery = sql.parallelQuery("SELECT `ip`, `value`, `created` FROM `antivpn_queue` ORDER BY `updated` ASC;");
    }

    private void onSQLData(SQLEventArgs e) {
        if (e.getUuid().equals(fetchQuery) || e.getUuid().equals(consensusFetchQuery)) {
            Exception lastEx = null;

            try (Jedis redis = RedisUtil.getRedis()) {
                // Iterate rows
                for (Object[] o : e.getData().data) {
                    try {
                        // Validate UUID/IP and remove bad data
                        if (!ValidationUtil.isValidIp((String) o[0])) {
                            if (e.getUuid().equals(fetchQuery)) {
                                if (redis != null) {
                                    String key = "avpn:" + (String) o[0];
                                    redis.del(key);
                                }
                                sql.parallelQuery("DELETE FROM `antivpn_queue` WHERE `ip`=?;", o[0]);
                            } else if (e.getUuid().equals(consensusFetchQuery)) {
                                if (redis != null) {
                                    String key = "avpn:consensus:" + (String) o[0];
                                    redis.del(key);
                                }
                                sql.parallelQuery("DELETE FROM `antivpn_consensus_queue` WHERE `ip`=?;", o[0]);
                            }

                            continue;
                        }

                        if (e.getUuid().equals(fetchQuery)) {
                            // Grab all data and convert to more useful object types
                            String ip = (String) o[0];
                            boolean value = ((Boolean) o[1]).booleanValue();
                            long created = ((Timestamp) o[2]).getTime();

                            // Set Redis, if available
                            if (redis != null) {
                                String key = "avpn:" + ip;
                                int offset = (int) Math.floorDiv((Config.sourceCacheTime + created) - System.currentTimeMillis(), 1000L);
                                if (offset > 0) {
                                    redis.setex(key, offset, String.valueOf(value));
                                } else {
                                    redis.del(key);
                                }
                            }

                            // Set cache, if available
                            IPCacheUtil.addToCache(ip, value, created, true);
                        } else if (e.getUuid().equals(consensusFetchQuery)) {
                            // Grab all data and convert to more useful object types
                            String ip = (String) o[0];
                            double value = ((Number) o[1]).doubleValue();
                            long created = ((Timestamp) o[2]).getTime();

                            // Set Redis, if available
                            if (redis != null) {
                                String key = "avpn:consensus:" + ip;
                                int offset = (int) Math.floorDiv((Config.sourceCacheTime + created) - System.currentTimeMillis(), 1000L);
                                if (offset > 0) {
                                    redis.setex(key, offset, String.valueOf(value));
                                } else {
                                    redis.del(key);
                                }
                            }

                            // Set cache, if available
                            IPCacheUtil.addToCache(ip, value, created, true);
                        }
                    } catch (Exception ex) {
                        IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
                        if (handler != null) {
                            handler.sendException(ex);
                        }
                        ex.printStackTrace();
                        lastEx = ex;
                    }
                }
            }

            if (e.getUuid().equals(fetchQuery)) {
                consensusFetchQuery = sql.parallelQuery("SELECT `ip`, `value`, `created` FROM `antivpn_consensus_queue` ORDER BY `updated` ASC;");
            } else if (e.getUuid().equals(consensusFetchQuery)) {
                queueQuery = sql.parallelQuery("DELETE FROM `antivpn_queue` WHERE `updated` <= CURRENT_TIMESTAMP() - INTERVAL 2 MINUTE;");
            }

            if (lastEx != null) {
                throw new RuntimeException(lastEx);
            }
        } else if (e.getUuid().equals(queueQuery)) {
            consensusQueueQuery = sql.parallelQuery("DELETE FROM `antivpn_consensus_queue` WHERE `updated` <= CURRENT_TIMESTAMP() - INTERVAL 2 MINUTE;");
        } else if (e.getUuid().equals(consensusQueueQuery)) {
            finalQuery1 = sql.parallelQuery("DELETE FROM `antivpn` WHERE CURRENT_TIMESTAMP() > `created` + INTERVAL ? * 0.001 SECOND;", Long.valueOf(Config.sourceCacheTime));
            finalQuery2 = sql.parallelQuery("DELETE FROM `antivpn_consensus` WHERE CURRENT_TIMESTAMP() > `created` + INTERVAL ? * 0.001 SECOND;", Long.valueOf(Config.sourceCacheTime));
        } else if (e.getUuid().equals(finalQuery1) || e.getUuid().equals(finalQuery2)) {
            if (e.getUuid().equals(finalQuery1)) {
                finalQuery1 = null;
            } else if (e.getUuid().equals(finalQuery2)) {
                finalQuery2 = null;
            }

            if (finalQuery1 == null && finalQuery2 == null) {
                sql.onError().detatch(sqlError);
                sql.onData().detatch(sqlError);

                onComplete().invoke(this, CompleteEventArgs.EMPTY);
            }
        }
    }

    private void onSQLError(SQLEventArgs e) {
        if (!e.getUuid().equals(fetchQuery) && !e.getUuid().equals(consensusFetchQuery) && !e.getUuid().equals(queueQuery) && !e.getUuid().equals(consensusQueueQuery)
            && !e.getUuid().equals(finalQuery1) && !e.getUuid().equals(finalQuery2)) {
            return;
        }

        IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
        if (handler != null) {
            handler.sendException(e.getSQLError().ex);
        }
        // Wrap in a new exception and print to console. We wrap so we know where the
        // error actually comes from
        new Exception(e.getSQLError().ex).printStackTrace();

        sql.onError().detatch(sqlError);
        sql.onData().detatch(sqlError);

        onComplete().invoke(this, CompleteEventArgs.EMPTY);

        throw new RuntimeException(e.getSQLError().ex);
    }
}

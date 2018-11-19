package me.egg82.antivpn.sql;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.core.ConsensusResult;
import me.egg82.antivpn.core.DataResult;
import me.egg82.antivpn.core.SQLFetchResult;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.core.SQLQueryResult;
import ninja.egg82.sql.SQL;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLite {
    private static final Logger logger = LoggerFactory.getLogger(SQLite.class);

    private SQLite() {}

    public static CompletableFuture<Void> createTables(SQL sql, ConfigurationNode storageConfigNode) {
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";

        return CompletableFuture.runAsync(() -> {
            try {
                SQLQueryResult query = sql.query("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='" + tablePrefix.substring(0, tablePrefix.length() - 1) + "';");
                if (query.getData().length > 0 && query.getData()[0].length > 0 && ((Number) query.getData()[0][0]).intValue() != 0) {
                    return;
                }

                sql.execute("CREATE TABLE `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` ("
                        + "`ip` TEXT(45) NOT NULL,"
                        + "`value` INTEGER(1) NOT NULL,"
                        + "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "UNIQUE(`ip`)"
                        + ");");
            } catch (SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }

            try {
                SQLQueryResult query = sql.query("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='" + tablePrefix + "consensus';");
                if (query.getData().length > 0 && query.getData()[0].length > 0 && ((Number) query.getData()[0][0]).intValue() != 0) {
                    return;
                }

                sql.execute("CREATE TABLE `" + tablePrefix + "consensus` ("
                        + "`ip` TEXT(45) NOT NULL,"
                        + "`value` DOUBLE NOT NULL,"
                        + "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "UNIQUE(`ip`)"
                        + ");");
            } catch (SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }
        });
    }

    public static CompletableFuture<SQLFetchResult> loadInfo(SQL sql, ConfigurationNode storageConfigNode) {
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";

        return CompletableFuture.supplyAsync(() -> {
            List<DataResult> data = new ArrayList<>();
            List<ConsensusResult> consensus = new ArrayList<>();
            List<String> removedKeys = new ArrayList<>();

            try {
                SQLQueryResult query = sql.query("SELECT `ip`, `value`, `created` FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "`;");

                // Iterate rows
                for (Object[] o : query.getData()) {
                    // Validate UUID/IP and remove bad data
                    if (!ValidationUtil.isValidIp((String) o[0])) {
                        removedKeys.add("avpn:" + o[0]);
                        sql.execute("DELETE FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE `ip`=?;", o[0]);
                        continue;
                    }

                    // Grab all data and convert to more useful object types
                    String ip = (String) o[0];
                    boolean value = (((Number) o[1]).intValue() == 0) ? false : true;
                    long created = getTime(o[2]).getTime();

                    data.add(new DataResult(ip, value, created));
                }
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            try {
                SQLQueryResult query = sql.query("SELECT `ip`, `value`, `created` FROM `" + tablePrefix + "consensus`;");

                // Iterate rows
                for (Object[] o : query.getData()) {
                    // Validate UUID/IP and remove bad data
                    if (!ValidationUtil.isValidIp((String) o[0])) {
                        removedKeys.add("avpn:consensus:" + o[0]);
                        sql.execute("DELETE FROM `" + tablePrefix + "consensus` WHERE `ip`=?;", o[0]);
                        continue;
                    }

                    // Grab all data and convert to more useful object types
                    String ip = (String) o[0];
                    double value = ((Number) o[1]).doubleValue();
                    long created = getTime(o[2]).getTime();

                    consensus.add(new ConsensusResult(ip, value, created));
                }
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return new SQLFetchResult(data.toArray(new DataResult[0]), consensus.toArray(new ConsensusResult[0]), removedKeys.toArray(new String[0]));
        });
    }

    public static CompletableFuture<Void> pruneTables(SQL sql, ConfigurationNode storageConfigNode, long sourceCacheTime) {
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";

        return CompletableFuture.runAsync(() -> {
            try {
                sql.execute("DELETE FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE CURRENT_TIMESTAMP > DATETIME(`created`, ?)", "+" + Math.floorDiv(sourceCacheTime, 1000L) + " seconds");
                sql.execute("DELETE FROM `" + tablePrefix + "consensus` WHERE CURRENT_TIMESTAMP > DATETIME(`created`, ?);", "+" + Math.floorDiv(sourceCacheTime, 1000L) + " seconds");
            } catch (SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }
        });
    }

    public static CompletableFuture<DataResult> getResult(String ip, SQL sql, ConfigurationNode storageConfigNode, long sourceCacheTime) {
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";

        return CompletableFuture.supplyAsync(() -> {
            DataResult result = null;

            try {
                SQLQueryResult query = sql.query("SELECT `value`, `created` FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE `ip`=? AND CURRENT_TIMESTAMP <= DATETIME(`created`, ?);", ip, "+" + Math.floorDiv(sourceCacheTime, 1000L) + " seconds");

                // Iterate rows
                for (Object[] o : query.getData()) {
                    // Grab all data and convert to more useful object types
                    boolean value = ((Number) o[0]).intValue() != 0;
                    long created = getTime(o[1]).getTime();

                    result = new DataResult(ip, value, created);
                }
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return result;
        });
    }

    public static CompletableFuture<ConsensusResult> getConsensus(String ip, SQL sql, ConfigurationNode storageConfigNode, long sourceCacheTime) {
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";

        return CompletableFuture.supplyAsync(() -> {
            ConsensusResult result = null;

            try {
                SQLQueryResult query = sql.query("SELECT `value`, `created` FROM `" + tablePrefix + "consensus`WHERE `ip`=? AND CURRENT_TIMESTAMP <= DATETIME(`created`, ?);", ip, "+" + Math.floorDiv(sourceCacheTime, 1000L) + " seconds");

                // Iterate rows
                for (Object[] o : query.getData()) {
                    // Grab all data and convert to more useful object types
                    double value = ((Number) o[0]).doubleValue();
                    long created = getTime(o[1]).getTime();

                    result = new ConsensusResult(ip, value, created);
                }
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return result;
        });
    }

    public static CompletableFuture<DataResult> update(SQL sql, ConfigurationNode storageConfigNode, String ip, boolean value) {
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";

        return CompletableFuture.supplyAsync(() -> {
            DataResult result = null;

            try {
                sql.execute("INSERT OR REPLACE INTO `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` (`ip`, `value`) VALUES (?, ?);", ip, (value) ? 1 : 0);
                SQLQueryResult query = sql.query("SELECT `created` FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE `ip`=?;", ip);

                Timestamp sqlCreated;

                for (Object[] o : query.getData()) {
                    sqlCreated = getTime(o[0]);
                    result = new DataResult(ip, value, sqlCreated.getTime());
                }
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return result;
        });
    }

    public static CompletableFuture<ConsensusResult> update(SQL sql, ConfigurationNode storageConfigNode, String ip, double value) {
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";

        return CompletableFuture.supplyAsync(() -> {
            ConsensusResult result = null;

            try {
                sql.execute("INSERT OR REPLACE INTO `" + tablePrefix + "consensus` (`ip`, `value`) VALUES (?, ?);", ip, value);
                SQLQueryResult query = sql.query("SELECT `created` FROM `" + tablePrefix + "consensus` WHERE `ip`=?;", ip);

                Timestamp sqlCreated;

                for (Object[] o : query.getData()) {
                    sqlCreated = getTime(o[0]);
                    result = new ConsensusResult(ip, value, sqlCreated.getTime());
                }
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return result;
        });
    }

    public static CompletableFuture<Void> set(String ip, boolean value, long created, SQL sql, ConfigurationNode storageConfigNode) {
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";

        return CompletableFuture.runAsync(() -> {
            try {
                sql.execute("INSERT OR REPLACE INTO `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` (`ip`, `value`, `created`) VALUES (?, ?, ?);", ip, (value) ? 1 : 0, created);
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }
        });
    }

    public static CompletableFuture<Void> set(String ip, double value, long created, SQL sql, ConfigurationNode storageConfigNode) {
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";

        return CompletableFuture.runAsync(() -> {
            try {
                sql.execute("INSERT OR REPLACE INTO `" + tablePrefix + "consensus` (`ip`, `value`, `created`) VALUES (?, ?, ?);", ip, value, created);
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }
        });
    }

    public static CompletableFuture<Void> delete(String ip, SQL sql, ConfigurationNode storageConfigNode) {
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";

        return CompletableFuture.runAsync(() -> {
            try {
                sql.execute("DELETE FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE `ip`=?;", ip);
                sql.execute("DELETE FROM `" + tablePrefix + "consensus` WHERE `ip`=?;", ip);
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }
        });
    }

    public static CompletableFuture<Long> getCurrentTime(SQL sql) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long start = System.currentTimeMillis();
                SQLQueryResult query = sql.query("SELECT CURRENT_TIMESTAMP;");

                for (Object[] o : query.getData()) {
                    return getTime(o[0]).getTime() + (System.currentTimeMillis() - start);
                }
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return -1L;
        });
    }

    private static Timestamp getTime(Object o) {
        if (o instanceof String) {
            return Timestamp.valueOf((String) o);
        } else if (o instanceof Number) {
            return new Timestamp(((Number) o).longValue());
        }
        return null;
    }
}

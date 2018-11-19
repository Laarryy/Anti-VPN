package me.egg82.antivpn.sql;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import me.egg82.antivpn.core.ConsensusResult;
import me.egg82.antivpn.core.DataResult;
import me.egg82.antivpn.core.SQLFetchResult;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.core.SQLQueryResult;
import ninja.egg82.sql.SQL;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySQL {
    private static final Logger logger = LoggerFactory.getLogger(MySQL.class);

    private MySQL() {}

    public static CompletableFuture<Void> createTables(SQL sql, ConfigurationNode storageConfigNode) {
        String databaseName = storageConfigNode.getNode("data", "database").getString();
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";

        return CompletableFuture.runAsync(() -> {
            try {
                SQLQueryResult query = sql.query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name='" + tablePrefix.substring(0, tablePrefix.length() - 1) + "';", databaseName);
                if (query.getData().length > 0 && query.getData()[0].length > 0 && ((Number) query.getData()[0][0]).intValue() != 0) {
                    return;
                }

                sql.execute("CREATE TABLE `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` ("
                        + "`ip` VARCHAR(45) NOT NULL,"
                        + "`value` BOOLEAN NOT NULL,"
                        + "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ");");
                sql.execute("ALTER TABLE `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` ADD UNIQUE (`ip`);");
            } catch (SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }

            try {
                SQLQueryResult query = sql.query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name='" + tablePrefix + "consensus';", databaseName);
                if (query.getData().length > 0 && query.getData()[0].length > 0 && ((Number) query.getData()[0][0]).intValue() != 0) {
                    return;
                }

                sql.execute("CREATE TABLE `" + tablePrefix + "consensus` ("
                        + "`ip` VARCHAR(45) NOT NULL,"
                        + "`value` DOUBLE NOT NULL,"
                        + "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ");");
                sql.execute("ALTER TABLE `" + tablePrefix + "consensus` ADD UNIQUE (`ip`);");
            } catch (SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }

            try {
                SQLQueryResult query = sql.query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name='" + tablePrefix + "queue';", databaseName);
                if (query.getData().length > 0 && query.getData()[0].length > 0 && ((Number) query.getData()[0][0]).intValue() != 0) {
                    return;
                }

                sql.execute("CREATE TABLE `" + tablePrefix + "queue` ("
                        + "`ip` VARCHAR(45) NOT NULL,"
                        + "`value` BOOLEAN NOT NULL,"
                        + "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "`updated` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ");");
                sql.execute("ALTER TABLE `" + tablePrefix + "queue` ADD UNIQUE (`ip`);");
            } catch (SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }

            try {
                SQLQueryResult query = sql.query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name='" + tablePrefix + "consensus_queue';", databaseName);
                if (query.getData().length > 0 && query.getData()[0].length > 0 && ((Number) query.getData()[0][0]).intValue() != 0) {
                    return;
                }

                sql.execute("CREATE TABLE `" + tablePrefix + "consensus_queue` ("
                        + "`ip` VARCHAR(45) NOT NULL,"
                        + "`value` DOUBLE NOT NULL,"
                        + "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "`updated` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ");");
                sql.execute("ALTER TABLE `" + tablePrefix + "consensus_queue` ADD UNIQUE (`ip`);");
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
                    boolean value = (Boolean) o[1];
                    long created = ((Timestamp) o[2]).getTime();

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
                    long created = ((Timestamp) o[2]).getTime();

                    consensus.add(new ConsensusResult(ip, value, created));
                }
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return new SQLFetchResult(data.toArray(new DataResult[0]), consensus.toArray(new ConsensusResult[0]), removedKeys.toArray(new String[0]));
        });
    }

    public static CompletableFuture<SQLFetchResult> fetchQueue(SQL sql, ConfigurationNode storageConfigNode, long sourceCacheTime) {
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";

        return CompletableFuture.supplyAsync(() -> {
            List<DataResult> data = new ArrayList<>();
            List<ConsensusResult> consensus = new ArrayList<>();
            List<String> removedKeys = new ArrayList<>();

            try {
                SQLQueryResult query = sql.query("SELECT `ip`, `value`, `created` FROM `" + tablePrefix + "queue` ORDER BY `updated` ASC;");

                // Iterate rows
                for (Object[] o : query.getData()) {
                    // Validate UUID/IP and remove bad data
                    if (!ValidationUtil.isValidIp((String) o[0])) {
                        removedKeys.add("avpn:" + o[0]);
                        sql.execute("DELETE FROM `" + tablePrefix + "queue` WHERE `ip`=?;", o[0]);
                        continue;
                    }

                    // Grab all data and convert to more useful object types
                    String ip = (String) o[0];
                    boolean value = ((Boolean) o[1]);
                    long created = ((Timestamp) o[2]).getTime();

                    data.add(new DataResult(ip, value, created));
                }
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            try {
                SQLQueryResult query = sql.query("SELECT `ip`, `value`, `created` FROM `" + tablePrefix + "consensus_queue` ORDER BY `updated` ASC;");

                // Iterate rows
                for (Object[] o : query.getData()) {
                    // Validate UUID/IP and remove bad data
                    if (!ValidationUtil.isValidIp((String) o[0])) {
                        removedKeys.add("avpn:consensus:" + o[0]);
                        sql.execute("DELETE FROM `" + tablePrefix + "consensus_queue` WHERE `ip`=?;", o[0]);
                        continue;
                    }

                    // Grab all data and convert to more useful object types
                    String ip = (String) o[0];
                    double value = ((Number) o[1]).doubleValue();
                    long created = ((Timestamp) o[2]).getTime();

                    consensus.add(new ConsensusResult(ip, value, created));
                }
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            try {
                sql.execute("DELETE FROM `" + tablePrefix + "queue` WHERE `updated` <= CURRENT_TIMESTAMP() - INTERVAL 2 MINUTE;");
                sql.execute("DELETE FROM `" + tablePrefix + "consensus_queue` WHERE `updated` <= CURRENT_TIMESTAMP() - INTERVAL 2 MINUTE;");
                sql.execute("DELETE FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE CURRENT_TIMESTAMP() > `created` + INTERVAL ? * 0.001 SECOND;", sourceCacheTime);
                sql.execute("DELETE FROM `" + tablePrefix + "consensus` WHERE CURRENT_TIMESTAMP() > `created` + INTERVAL ? * 0.001 SECOND;", sourceCacheTime);
            } catch (SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return new SQLFetchResult(data.toArray(new DataResult[0]), consensus.toArray(new ConsensusResult[0]), removedKeys.toArray(new String[0]));
        });
    }

    public static CompletableFuture<DataResult> getResult(String ip, SQL sql, ConfigurationNode storageConfigNode, long sourceCacheTime) {
        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";

        return CompletableFuture.supplyAsync(() -> {
            DataResult result = null;

            try {
                SQLQueryResult query = sql.query("SELECT `value`, `created` FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE `ip`=? AND CURRENT_TIMESTAMP() <= `created` + INTERVAL ? * 0.001 SECOND;", ip, sourceCacheTime);

                // Iterate rows
                for (Object[] o : query.getData()) {
                    // Grab all data and convert to more useful object types
                    boolean value = (Boolean) o[0];
                    long created = ((Timestamp) o[1]).getTime();

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
                SQLQueryResult query = sql.query("SELECT `value`, `created` FROM `" + tablePrefix + "consensus` WHERE `ip`=? AND CURRENT_TIMESTAMP() <= `created` + INTERVAL ? * 0.001 SECOND;", ip, sourceCacheTime);

                // Iterate rows
                for (Object[] o : query.getData()) {
                    // Grab all data and convert to more useful object types
                    double value = ((Number) o[0]).doubleValue();
                    long created = ((Timestamp) o[1]).getTime();

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
                sql.execute("INSERT INTO `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` (`ip`, `value`) VALUES(?, ?) ON DUPLICATE KEY UPDATE `value`=?, `created`=CURRENT_TIMESTAMP();", ip, (value) ? 1 : 0, (value) ? 1 : 0);
                SQLQueryResult query = sql.query("SELECT `created` FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE `ip`=?;", ip);

                Timestamp sqlCreated = null;
                Timestamp updated = new Timestamp(System.currentTimeMillis());

                for (Object[] o : query.getData()) {
                    sqlCreated = (Timestamp) o[0];
                    result = new DataResult(ip, value, sqlCreated.getTime());
                }

                if (sqlCreated != null) {
                    sql.query("INSERT INTO `" + tablePrefix + "queue` (`ip`, `value`, `created`, `updated`) VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE `updated`=?;", ip, (value) ? 1 : 0, sqlCreated, updated, updated);
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
                sql.execute("INSERT INTO `" + tablePrefix + "consensus` (`ip`, `value`) VALUES(?, ?) ON DUPLICATE KEY UPDATE `value`=?, `created`=CURRENT_TIMESTAMP();", ip, value, value);
                SQLQueryResult query = sql.query("SELECT `created` FROM `" + tablePrefix + "consensus` WHERE `ip`=?;", ip);

                Timestamp sqlCreated = null;
                Timestamp updated = new Timestamp(System.currentTimeMillis());

                for (Object[] o : query.getData()) {
                    sqlCreated = (Timestamp) o[0];
                    result = new ConsensusResult(ip, value, sqlCreated.getTime());
                }

                if (sqlCreated != null) {
                    sql.query("INSERT INTO `" + tablePrefix + "consensus_queue` (`ip`, `value`, `created`, `updated`) VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE `updated`=?;", ip, value, sqlCreated, updated, updated);
                }
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return result;
        });
    }

    public static CompletableFuture<Long> getCurrentTime(SQL sql) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long start = System.currentTimeMillis();
                SQLQueryResult query = sql.query("SELECT CURRENT_TIMESTAMP();");

                for (Object[] o : query.getData()) {
                    return ((Timestamp) o[0]).getTime() + (System.currentTimeMillis() - start);
                }
            } catch (SQLException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return -1L;
        });
    }
}

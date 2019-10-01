package me.egg82.antivpn.sql;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.core.ConsensusResult;
import me.egg82.antivpn.core.DataResult;
import me.egg82.antivpn.core.SQLFetchResult;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.core.SQLQueryResult;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySQL {
    private static final Logger logger = LoggerFactory.getLogger(MySQL.class);

    private MySQL() {}

    public static void createTables() throws APIException, SQLException {
        String tablePrefix = getTablePrefix();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        if (!tableExists(tablePrefix.substring(0, tablePrefix.length() - 1))) {
            cachedConfig.get().getSQL().execute("CREATE TABLE `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` ("
                    + "`ip` VARCHAR(45) NOT NULL,"
                    + "`value` BOOLEAN NOT NULL,"
                    + "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                    + ");");
            cachedConfig.get().getSQL().execute("ALTER TABLE `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` ADD UNIQUE (`ip`);");
        }

        if (!tableExists(tablePrefix + "consensus")) {
            cachedConfig.get().getSQL().execute("CREATE TABLE `" + tablePrefix + "consensus` ("
                    + "`ip` VARCHAR(45) NOT NULL,"
                    + "`value` DOUBLE NOT NULL,"
                    + "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                    + ");");
            cachedConfig.get().getSQL().execute("ALTER TABLE `" + tablePrefix + "consensus` ADD UNIQUE (`ip`);");
        }

        if (!tableExists(tablePrefix + "queue")) {
            cachedConfig.get().getSQL().execute("CREATE TABLE `" + tablePrefix + "queue` ("
                    + "`ip` VARCHAR(45) NOT NULL,"
                    + "`value` BOOLEAN NOT NULL,"
                    + "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "`updated` TIMESTAMP NOT NULL"
                    + ");");
            cachedConfig.get().getSQL().execute("ALTER TABLE `" + tablePrefix + "queue` ADD UNIQUE (`ip`);");
        }

        if (!tableExists(tablePrefix + "consensus_queue")) {
            cachedConfig.get().getSQL().execute("CREATE TABLE `" + tablePrefix + "consensus_queue` ("
                    + "`ip` VARCHAR(45) NOT NULL,"
                    + "`value` DOUBLE NOT NULL,"
                    + "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "`updated` TIMESTAMP NOT NULL"
                    + ");");
            cachedConfig.get().getSQL().execute("ALTER TABLE `" + tablePrefix + "consensus_queue` ADD UNIQUE (`ip`);");
        }
    }

    public static SQLFetchResult loadInfo() throws APIException, SQLException {
        String tablePrefix = getTablePrefix();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        List<DataResult> data = new ArrayList<>();
        List<ConsensusResult> consensus = new ArrayList<>();
        List<String> removedKeys = new ArrayList<>();

        try {
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT `ip`, `value`, `created` FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "`;");

            // Iterate rows
            for (Object[] o : query.getData()) {
                // Validate UUID/IP and remove bad data
                if (!ValidationUtil.isValidIp((String) o[0])) {
                    removedKeys.add("avpn:" + o[0]);
                    cachedConfig.get().getSQL().execute("DELETE FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE `ip`=?;", o[0]);
                    continue;
                }

                // Grab all data and convert to more useful object types
                String ip = (String) o[0];
                boolean value = (Boolean) o[1];
                long created = ((Timestamp) o[2]).getTime();

                data.add(new DataResult(ip, value, created));
            }
        } catch (ClassCastException ex) {
            logger.error(ex.getMessage(), ex);
        }

        try {
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT `ip`, `value`, `created` FROM `" + tablePrefix + "consensus`;");

            // Iterate rows
            for (Object[] o : query.getData()) {
                // Validate UUID/IP and remove bad data
                if (!ValidationUtil.isValidIp((String) o[0])) {
                    removedKeys.add("avpn:consensus:" + o[0]);
                    cachedConfig.get().getSQL().execute("DELETE FROM `" + tablePrefix + "consensus` WHERE `ip`=?;", o[0]);
                    continue;
                }

                // Grab all data and convert to more useful object types
                String ip = (String) o[0];
                double value = ((Number) o[1]).doubleValue();
                long created = ((Timestamp) o[2]).getTime();

                consensus.add(new ConsensusResult(ip, value, created));
            }
        } catch (ClassCastException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return new SQLFetchResult(
                data.toArray(new DataResult[0]),
                consensus.toArray(new ConsensusResult[0]),
                removedKeys.toArray(new String[0])
        );
    }

    public static SQLFetchResult fetchQueue() throws APIException, SQLException {
        String tablePrefix = getTablePrefix();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        List<DataResult> data = new ArrayList<>();
        List<ConsensusResult> consensus = new ArrayList<>();
        List<String> removedKeys = new ArrayList<>();

        try {
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT `ip`, `value`, `created` FROM `" + tablePrefix + "queue` ORDER BY `updated` ASC;");

            // Iterate rows
            for (Object[] o : query.getData()) {
                // Validate UUID/IP and remove bad data
                if (!ValidationUtil.isValidIp((String) o[0])) {
                    removedKeys.add("avpn:" + o[0]);
                    cachedConfig.get().getSQL().execute("DELETE FROM `" + tablePrefix + "queue` WHERE `ip`=?;", o[0]);
                    continue;
                }

                // Grab all data and convert to more useful object types
                String ip = (String) o[0];
                boolean value = ((Boolean) o[1]);
                long created = ((Timestamp) o[2]).getTime();

                data.add(new DataResult(ip, value, created));
            }
        } catch (ClassCastException ex) {
            logger.error(ex.getMessage(), ex);
        }

        try {
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT `ip`, `value`, `created` FROM `" + tablePrefix + "consensus_queue` ORDER BY `updated` ASC;");

            // Iterate rows
            for (Object[] o : query.getData()) {
                // Validate UUID/IP and remove bad data
                if (!ValidationUtil.isValidIp((String) o[0])) {
                    removedKeys.add("avpn:consensus:" + o[0]);
                    cachedConfig.get().getSQL().execute("DELETE FROM `" + tablePrefix + "consensus_queue` WHERE `ip`=?;", o[0]);
                    continue;
                }

                // Grab all data and convert to more useful object types
                String ip = (String) o[0];
                double value = ((Number) o[1]).doubleValue();
                long created = ((Timestamp) o[2]).getTime();

                consensus.add(new ConsensusResult(ip, value, created));
            }
        } catch (ClassCastException ex) {
            logger.error(ex.getMessage(), ex);
        }

        cachedConfig.get().getSQL().execute("DELETE FROM `" + tablePrefix + "queue` WHERE `updated` <= CURRENT_TIMESTAMP() - INTERVAL 2 MINUTE;");
        cachedConfig.get().getSQL().execute("DELETE FROM `" + tablePrefix + "consensus_queue` WHERE `updated` <= CURRENT_TIMESTAMP() - INTERVAL 2 MINUTE;");
        cachedConfig.get().getSQL().execute("DELETE FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE CURRENT_TIMESTAMP() > `created` + INTERVAL ? * 0.001 SECOND;", cachedConfig.get().getSourceCacheTime());
        cachedConfig.get().getSQL().execute("DELETE FROM `" + tablePrefix + "consensus` WHERE CURRENT_TIMESTAMP() > `created` + INTERVAL ? * 0.001 SECOND;", cachedConfig.get().getSourceCacheTime());

        return new SQLFetchResult(
                data.toArray(new DataResult[0]),
                consensus.toArray(new ConsensusResult[0]),
                removedKeys.toArray(new String[0])
        );
    }

    public static Optional<DataResult> getResult(String ip) throws APIException, SQLException {
        String tablePrefix = getTablePrefix();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        DataResult result = null;

        try {
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT `value`, `created` FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE `ip`=? AND CURRENT_TIMESTAMP() <= `created` + INTERVAL ? * 0.001 SECOND;", ip, cachedConfig.get().getSourceCacheTime());

            // Iterate rows
            for (Object[] o : query.getData()) {
                // Grab all data and convert to more useful object types
                boolean value = (Boolean) o[0];
                long created = ((Timestamp) o[1]).getTime();

                result = new DataResult(ip, value, created);
            }
        } catch (ClassCastException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return Optional.ofNullable(result);
    }

    public static Optional<ConsensusResult> getConsensus(String ip) throws APIException, SQLException {
        String tablePrefix = getTablePrefix();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        ConsensusResult result = null;

        try {
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT `value`, `created` FROM `" + tablePrefix + "consensus` WHERE `ip`=? AND CURRENT_TIMESTAMP() <= `created` + INTERVAL ? * 0.001 SECOND;", ip, cachedConfig.get().getSourceCacheTime());

            // Iterate rows
            for (Object[] o : query.getData()) {
                // Grab all data and convert to more useful object types
                double value = ((Number) o[0]).doubleValue();
                long created = ((Timestamp) o[1]).getTime();

                result = new ConsensusResult(ip, value, created);
            }
        } catch (ClassCastException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return Optional.ofNullable(result);
    }

    public static DataResult update(String ip, boolean value) throws APIException, SQLException {
        String tablePrefix = getTablePrefix();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        DataResult result = null;

        try {
            cachedConfig.get().getSQL().execute("INSERT INTO `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` (`ip`, `value`, `updated`) VALUES(?, ?, CURRENT_TIMESTAMP()) ON DUPLICATE KEY UPDATE `value`=?, `created`=CURRENT_TIMESTAMP();", ip, (value) ? 1 : 0, (value) ? 1 : 0);
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT `created` FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE `ip`=?;", ip);

            Timestamp sqlCreated = null;
            Timestamp updated = new Timestamp(System.currentTimeMillis());

            for (Object[] o : query.getData()) {
                sqlCreated = (Timestamp) o[0];
                result = new DataResult(ip, value, sqlCreated.getTime());
            }

            if (sqlCreated != null) {
                cachedConfig.get().getSQL().execute("INSERT INTO `" + tablePrefix + "queue` (`ip`, `value`, `created`, `updated`) VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE `updated`=?;", ip, (value) ? 1 : 0, sqlCreated, updated, updated);
            }
        } catch (ClassCastException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return result;
    }

    public static ConsensusResult update(String ip, double value) throws APIException, SQLException {
        String tablePrefix = getTablePrefix();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        ConsensusResult result = null;

        try {
            cachedConfig.get().getSQL().execute("INSERT INTO `" + tablePrefix + "consensus` (`ip`, `value`) VALUES(?, ?) ON DUPLICATE KEY UPDATE `value`=?, `created`=CURRENT_TIMESTAMP();", ip, value, value);
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT `created` FROM `" + tablePrefix + "consensus` WHERE `ip`=?;", ip);

            Timestamp sqlCreated = null;
            Timestamp updated = new Timestamp(System.currentTimeMillis());

            for (Object[] o : query.getData()) {
                sqlCreated = (Timestamp) o[0];
                result = new ConsensusResult(ip, value, sqlCreated.getTime());
            }

            if (sqlCreated != null) {
                cachedConfig.get().getSQL().execute("INSERT INTO `" + tablePrefix + "consensus_queue` (`ip`, `value`, `created`, `updated`) VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE `updated`=?;", ip, value, sqlCreated, updated, updated);
            }
        } catch (ClassCastException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return result;
    }

    public static long getCurrentTime() throws APIException, SQLException {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        try {
            long start = System.currentTimeMillis();
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT CURRENT_TIMESTAMP();");

            for (Object[] o : query.getData()) {
                return ((Timestamp) o[0]).getTime() + (System.currentTimeMillis() - start);
            }
        } catch (ClassCastException ex) {
            throw new APIException(true, ex);
        }

        throw new APIException(true, "Could not get time from SQL.");
    }

    private static boolean tableExists(String tableName) throws APIException, SQLException {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        ConfigurationNode storageConfigNode = ConfigUtil.getStorageNodeOrNull();

        if (!cachedConfig.isPresent() || storageConfigNode == null) {
            throw new APIException(true, "Could not get required configuration.");
        }

        String databaseName = storageConfigNode.getNode("data", "database").getString();

        SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name=?;", databaseName, tableName);
        return query.getData().length > 0 && query.getData()[0].length > 0 && ((Number) query.getData()[0][0]).intValue() != 0;
    }

    private static String getTablePrefix() throws APIException {
        ConfigurationNode storageConfigNode = ConfigUtil.getStorageNodeOrNull();

        if (storageConfigNode == null) {
            throw new APIException(true, "Could not get required configuration.");
        }

        String tablePrefix = !storageConfigNode.getNode("data", "prefix").getString("").isEmpty() ? storageConfigNode.getNode("data", "prefix").getString() : "antivpn_";
        if (tablePrefix.charAt(tablePrefix.length() - 1) != '_') {
            tablePrefix = tablePrefix + "_";
        }
        return tablePrefix;
    }
}

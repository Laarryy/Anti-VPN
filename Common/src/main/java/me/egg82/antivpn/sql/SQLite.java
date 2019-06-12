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

public class SQLite {
    private static final Logger logger = LoggerFactory.getLogger(SQLite.class);

    private SQLite() {}

    public static void createTables() throws APIException, SQLException {
        String tablePrefix = getTablePrefix();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        if (!tableExists(tablePrefix.substring(0, tablePrefix.length() - 1))) {
            cachedConfig.get().getSQL().execute("CREATE TABLE `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` ("
                    + "`ip` TEXT(45) NOT NULL,"
                    + "`value` INTEGER(1) NOT NULL,"
                    + "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "UNIQUE(`ip`)"
                    + ");");
        }

        if (!tableExists(tablePrefix + "consensus")) {
            cachedConfig.get().getSQL().execute("CREATE TABLE `" + tablePrefix + "consensus` ("
                    + "`ip` TEXT(45) NOT NULL,"
                    + "`value` DOUBLE NOT NULL,"
                    + "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "UNIQUE(`ip`)"
                    + ");");
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
                boolean value = (((Number) o[1]).intValue() == 0) ? false : true;
                long created = getTime(o[2]).getTime();

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
                long created = getTime(o[2]).getTime();

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

    public static void pruneTables() throws APIException, SQLException {
        String tablePrefix = getTablePrefix();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        cachedConfig.get().getSQL().execute("DELETE FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE CURRENT_TIMESTAMP > DATETIME(`created`, ?)", "+" + Math.floorDiv(cachedConfig.get().getSourceCacheTime(), 1000L) + " seconds");
        cachedConfig.get().getSQL().execute("DELETE FROM `" + tablePrefix + "consensus` WHERE CURRENT_TIMESTAMP > DATETIME(`created`, ?);", "+" + Math.floorDiv(cachedConfig.get().getSourceCacheTime(), 1000L) + " seconds");
    }

    public static Optional<DataResult> getResult(String ip) throws APIException, SQLException {
        String tablePrefix = getTablePrefix();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        DataResult result = null;

        try {
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT `value`, `created` FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE `ip`=? AND CURRENT_TIMESTAMP <= DATETIME(`created`, ?);", ip, "+" + Math.floorDiv(cachedConfig.get().getSourceCacheTime(), 1000L) + " seconds");

            // Iterate rows
            for (Object[] o : query.getData()) {
                // Grab all data and convert to more useful object types
                boolean value = ((Number) o[0]).intValue() != 0;
                long created = getTime(o[1]).getTime();

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
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT `value`, `created` FROM `" + tablePrefix + "consensus`WHERE `ip`=? AND CURRENT_TIMESTAMP <= DATETIME(`created`, ?);", ip, "+" + Math.floorDiv(cachedConfig.get().getSourceCacheTime(), 1000L) + " seconds");

            // Iterate rows
            for (Object[] o : query.getData()) {
                // Grab all data and convert to more useful object types
                double value = ((Number) o[0]).doubleValue();
                long created = getTime(o[1]).getTime();

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
            cachedConfig.get().getSQL().execute("INSERT OR REPLACE INTO `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` (`ip`, `value`) VALUES (?, ?);", ip, (value) ? 1 : 0);
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT `created` FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE `ip`=?;", ip);

            Timestamp sqlCreated;

            for (Object[] o : query.getData()) {
                sqlCreated = getTime(o[0]);
                result = new DataResult(ip, value, sqlCreated.getTime());
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
            cachedConfig.get().getSQL().execute("INSERT OR REPLACE INTO `" + tablePrefix + "consensus` (`ip`, `value`) VALUES (?, ?);", ip, value);
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT `created` FROM `" + tablePrefix + "consensus` WHERE `ip`=?;", ip);

            Timestamp sqlCreated;

            for (Object[] o : query.getData()) {
                sqlCreated = getTime(o[0]);
                result = new ConsensusResult(ip, value, sqlCreated.getTime());
            }
        } catch (ClassCastException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return result;
    }

    public static void set(String ip, boolean value, long created) throws APIException, SQLException {
        String tablePrefix = getTablePrefix();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        cachedConfig.get().getSQL().execute("INSERT OR REPLACE INTO `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` (`ip`, `value`, `created`) VALUES (?, ?, ?);", ip, (value) ? 1 : 0, created);
    }

    public static void set(String ip, double value, long created) throws APIException, SQLException {
        String tablePrefix = getTablePrefix();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        cachedConfig.get().getSQL().execute("INSERT OR REPLACE INTO `" + tablePrefix + "consensus` (`ip`, `value`, `created`) VALUES (?, ?, ?);", ip, value, created);
    }

    public static void delete(String ip) throws APIException, SQLException {
        String tablePrefix = getTablePrefix();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        cachedConfig.get().getSQL().execute("DELETE FROM `" + tablePrefix.substring(0, tablePrefix.length() - 1) + "` WHERE `ip`=?;", ip);
        cachedConfig.get().getSQL().execute("DELETE FROM `" + tablePrefix + "consensus` WHERE `ip`=?;", ip);
    }

    public static long getCurrentTime() throws APIException, SQLException {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get required configuration.");
        }

        try {
            long start = System.currentTimeMillis();
            SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT CURRENT_TIMESTAMP;");

            for (Object[] o : query.getData()) {
                return getTime(o[0]).getTime() + (System.currentTimeMillis() - start);
            }
        } catch (ClassCastException ex) {
            throw new APIException(true, ex);
        }

        throw new APIException(true, "Could not get time from SQL.");
    }

    private static Timestamp getTime(Object o) throws APIException {
        if (o instanceof String) {
            return Timestamp.valueOf((String) o);
        } else if (o instanceof Number) {
            return new Timestamp(((Number) o).longValue());
        }
        throw new APIException(true, "Could not parse time.");
    }

    private static boolean tableExists(String tableName) throws APIException, SQLException {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        ConfigurationNode storageConfigNode = ConfigUtil.getStorageNodeOrNull();

        if (!cachedConfig.isPresent() || storageConfigNode == null) {
            throw new APIException(true, "Could not get required configuration.");
        }

        SQLQueryResult query = cachedConfig.get().getSQL().query("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?;", tableName);
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

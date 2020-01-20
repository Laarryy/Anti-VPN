package me.egg82.antivpn.storage;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.zaxxer.hikari.HikariConfig;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import me.egg82.antivpn.core.*;
import me.egg82.antivpn.services.StorageHandler;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.core.SQLExecuteResult;
import ninja.egg82.core.SQLQueryResult;
import ninja.egg82.sql.SQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteErrorCode;

public class SQLite extends AbstractSQL {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LoadingCache<String, Long> longIPIDCache = Caffeine.newBuilder().build(this::getLongIPIDExpensive);
    private final LoadingCache<UUID, Long> longPlayerIDCache = Caffeine.newBuilder().build(this::getLongPlayerIDExpensive);

    private volatile long lastVPNID;
    private volatile long lastMCLeaksID;
    private StorageHandler handler;

    private SQLite() { }

    private volatile boolean closed = false;

    public void close() {
        closed = true;
        sql.close();
    }

    public boolean isClosed() { return closed || sql.isClosed(); }

    public static SQLite.Builder builder(StorageHandler handler) { return new SQLite.Builder(handler); }

    public static class Builder {
        private final SQLite result = new SQLite();
        private final HikariConfig config = new HikariConfig();

        private Builder(StorageHandler handler) {
            if (handler == null) {
                throw new IllegalArgumentException("handler cannot be null.");
            }

            result.handler = handler;

            // Baseline
            config.setConnectionTestQuery("SELECT 1;");
            config.setAutoCommit(true);
            config.addDataSourceProperty("useLegacyDatetimeCode", "false");
            config.addDataSourceProperty("serverTimezone", "UTC");
        }

        public SQLite.Builder file(File file, String prefix) {
            config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            result.database = "";
            result.prefix = prefix;
            return this;
        }

        public SQLite.Builder options(String options) throws IOException {
            options = options.charAt(0) == '?' ? options.substring(1) : options;
            Properties p = new Properties();
            p.load(new StringReader(options.replace("&", "\n")));
            config.setDataSourceProperties(p);
            return this;
        }

        public SQLite.Builder poolSize(int min, int max) {
            config.setMaximumPoolSize(max);
            config.setMinimumIdle(min);
            return this;
        }

        public SQLite.Builder life(long lifetime, long timeout) {
            config.setMaxLifetime(lifetime);
            config.setConnectionTimeout(timeout);
            return this;
        }

        public SQLite build() throws IOException, StorageException {
            result.sql = new SQL(config);
            SQLVersionUtil.conformVersion(result, "sqlite");
            result.lastVPNID = getLastVPNID();
            result.lastMCLeaksID = getLastMCLeaksID();
            return result;
        }

        private long getLastVPNID() throws StorageException {
            SQLQueryResult r;
            try {
                r = result.sql.query("SELECT MAX(`id`) FROM `" + result.prefix + "vpn_values`;");
            } catch (SQLException ex) {
                throw new StorageException(false, ex);
            }
            if (r.getData().length != 1) {
                throw new StorageException(false, "Could not get VPN IDs.");
            }
            return r.getData()[0][0] != null ? ((Number) r.getData()[0][0]).longValue() : 0;
        }

        private long getLastMCLeaksID() throws StorageException {
            SQLQueryResult r;
            try {
                r = result.sql.query("SELECT MAX(`id`) FROM `" + result.prefix + "mcleaks_values`;");
            } catch (SQLException ex) {
                throw new StorageException(false, ex);
            }
            if (r.getData().length != 1) {
                throw new StorageException(false, "Could not get MCLeaks IDs.");
            }
            return r.getData()[0][0] != null ? ((Number) r.getData()[0][0]).longValue() : 0;
        }
    }

    public Set<VPNResult> getVPNQueue() throws StorageException {
        Set<VPNResult> retVal = new LinkedHashSet<>();
        SQLQueryResult result;
        try {
            result = sql.query(
                        "SELECT" +
                        "  `v`.`id`," +
                        "  `i`.`ip` AS `ip`," +
                        "  `v`.`cascade`," +
                        "  `v`.`consensus`," +
                        "  `v`.`created`" +
                        "FROM `" + prefix + "vpn_values` `v`" +
                        "JOIN `" + prefix + "ips` `i` ON `i`.`id` = `v`.`ip_id`" +
                        "WHERE `v`.`id` > ?;",
                    lastVPNID);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        for (Object[] row : result.getData()) {
            VPNResult r = getVPNResult(row);
            if (r != null) {
                lastVPNID = r.getID();
                retVal.add(r);
            }
        }
        return retVal;
    }

    public Set<MCLeaksResult> getMCLeaksQueue() throws StorageException {
        Set<MCLeaksResult> retVal = new LinkedHashSet<>();
        SQLQueryResult result;
        try {
            result = sql.query(
                    "SELECT" +
                        "  `v`.`id`," +
                        "  `p`.`uuid` AS `player_id`," +
                        "  `v`.`result`," +
                        "  `v`.`created`" +
                        "FROM `" + prefix + "mcleaks_values` `v`" +
                        "JOIN `" + prefix + "players` `p` ON `p`.`id` = `v`.`player_id`" +
                        "WHERE `v`.`id` > ?;",
                    lastMCLeaksID);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        for (Object[] row : result.getData()) {
            MCLeaksResult r = getMCLeaksResult(row);
            if (r != null) {
                lastMCLeaksID = r.getID();
                retVal.add(r);
            }
        }
        return retVal;
    }

    public VPNResult getVPNByIP(String ip, long cacheTimeMillis) throws StorageException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        long longIPID = longIPIDCache.get(ip);
        SQLQueryResult result;
        try {
            result = sql.query(
                    "SELECT" +
                        "  `v`.`id`," +
                        "  `i`.`ip` AS `ip`," +
                        "  `v`.`cascade`," +
                        "  `v`.`consensus`," +
                        "  `v`.`created`" +
                        "FROM `" + prefix + "vpn_values` `v`" +
                        "JOIN `" + prefix + "ips` `i` ON `i`.`id` = `v`.`ip_id`" +
                        "WHERE `v`.`created` >= DATETIME(CURRENT_TIMESTAMP, ?) AND `v`.`ip_id` = ?;",
                    "-" + (cacheTimeMillis / 1000L) + " seconds", longIPID);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        if (result.getData().length == 1) {
            return getVPNResult(result.getData()[0]);
        }
        return null;
    }

    public MCLeaksResult getMCLeaksByPlayer(UUID playerID, long cacheTimeMillis) throws StorageException {
        if (playerID == null) {
            throw new IllegalArgumentException("playerID cannot be null.");
        }

        long longPlayerID = longPlayerIDCache.get(playerID);
        SQLQueryResult result;
        try {
            result = sql.query(
                    "SELECT" +
                        "  `v`.`id`," +
                        "  `p`.`uuid` AS `player_id`," +
                        "  `v`.`result`," +
                        "  `v`.`created`" +
                        "FROM `" + prefix + "mcleaks_values` `v`" +
                        "JOIN `" + prefix + "players` `p` ON `p`.`id` = `v`.`player_id`" +
                        "WHERE `v`.`created` >= DATETIME(CURRENT_TIMESTAMP, ?) AND `v`.`player_id` = ?;",
                    "-" + (cacheTimeMillis / 1000L) + " seconds", longPlayerID);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        if (result.getData().length == 1) {
            return getMCLeaksResult(result.getData()[0]);
        }
        return null;
    }

    public PostVPNResult postVPN(String ip, Optional<Boolean> cascade, Optional<Double> consensus) throws StorageException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }
        if (cascade == null) {
            throw new IllegalArgumentException("cascade cannot be null.");
        }
        if (consensus == null) {
            throw new IllegalArgumentException("consensus cannot be null.");
        }

        long longIPID = longIPIDCache.get(ip);
        SQLExecuteResult result;
        try {
            result = sql.execute("INSERT INTO `" + prefix + "vpn_values` (`ip_id`, `cascade`, `consensus`) VALUES (?, ?, ?);", longIPID, cascade.orElse(null), consensus.orElse(null));
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        if (result.getAutoGeneratedKeys().length != 1) {
            throw new StorageException(false, "Could not get generated keys from inserted post.");
        }

        long id = ((Number) result.getAutoGeneratedKeys()[0]).longValue();

        SQLQueryResult query;
        try {
            query = sql.query("SELECT `created` FROM `" + prefix + "vpn_values` WHERE `id`=?;", id);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        if (query.getData().length != 1) {
            throw new StorageException(false, "Could not get date from inserted value.");
        }

        return new PostVPNResult(
                id,
                longIPID,
                ip,
                cascade,
                consensus,
                getTime(query.getData()[0][0]).getTime()
        );
    }

    public PostMCLeaksResult postMCLeaks(UUID playerID, boolean value) throws StorageException {
        if (playerID == null) {
            throw new IllegalArgumentException("playerID cannot be null.");
        }

        long longPlayerID = longPlayerIDCache.get(playerID);
        SQLExecuteResult result;
        try {
            result = sql.execute("INSERT INTO `" + prefix + "mcleaks_values` (`player_id`, `result`) VALUES (?, ?);", longPlayerID, value);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        if (result.getAutoGeneratedKeys().length != 1) {
            throw new StorageException(false, "Could not get generated keys from inserted post.");
        }

        long id = ((Number) result.getAutoGeneratedKeys()[0]).longValue();

        SQLQueryResult query;
        try {
            query = sql.query("SELECT `created` FROM `" + prefix + "mcleaks_values` WHERE `id`=?;", id);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        if (query.getData().length != 1) {
            throw new StorageException(false, "Could not get date from inserted value.");
        }

        return new PostMCLeaksResult(
                id,
                longPlayerID,
                playerID,
                value,
                getTime(query.getData()[0][0]).getTime()
        );
    }

    public void setIPRaw(long longIPID, String ip) throws StorageException {
        try {
            sql.execute("INSERT INTO `" + prefix + "ips` (`id`, `ip`) VALUES (?, ?) ON CONFLICT(`id`) DO UPDATE `ip`=?, ON CONFLICT(`ip`) DO UPDATE SET `id`=?;", longIPID, ip, ip, longIPID);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        longIPIDCache.put(ip, longIPID);
    }

    public void setPlayerRaw(long longPlayerID, UUID playerID) throws StorageException {
        try {
            sql.execute("INSERT INTO `" + prefix + "players` (`id`, `uuid`) VALUES (?, ?) ON CONFLICT(`id`) DO UPDATE SET `uuid`=? ON CONFLICT(`uuid`) DO UPDATE SET `id`=?;", longPlayerID, playerID.toString(), playerID.toString(), longPlayerID);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        longPlayerIDCache.put(playerID, longPlayerID);
    }

    public void postVPNRaw(long id, long longIPID, Optional<Boolean> cascade, Optional<Double> consensus, long created) throws StorageException {
        try {
            sql.execute("INSERT OR IGNORE INTO `" + prefix + "vpn_values` (`id`, `ip_id`, `cascade`, `consensus`, `created`) VALUES (?, ?, ?, ?, ?);", id, longIPID, cascade.orElse(null), consensus.orElse(null), new Timestamp(created));
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public void postMCLeaksRaw(long id, long longPlayerID, boolean value, long created) throws StorageException {
        try {
            sql.execute("INSERT OR IGNORE INTO `" + prefix + "mcleaks_values` (`id`, `player_id`, `result`, `created`) VALUES (?, ?, ?, ?);", id, longPlayerID, value, new Timestamp(created));
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    protected void setKey(String key, String value) throws SQLException { sql.execute("INSERT INTO `" + prefix + "data` (`key`, `value`) VALUES (?, ?) ON CONFLICT(`key`) DO UPDATE SET `value`=?;", key, value, value); }

    protected double getDouble(String key) throws SQLException {
        SQLQueryResult result = sql.query("SELECT `value` FROM `" + prefix + "data` WHERE `key`=?;", key);
        if (result.getData().length == 1) {
            return Double.parseDouble((String) result.getData()[0][0]);
        }
        return -1.0d;
    }

    public long getLongIPID(String ip) { return longIPIDCache.get(ip); }

    public long getLongPlayerID(UUID playerID) { return longPlayerIDCache.get(playerID); }

    public Set<IPResult> dumpIPs(long begin, int size) throws StorageException {
        Set<IPResult> retVal = new LinkedHashSet<>();

        SQLQueryResult result;
        try {
            result = sql.query("SELECT `id`, `ip` FROM `" + prefix + "ips` LIMIT ?, ?;", begin - 1, size);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }

        for (Object[] row : result.getData()) {
            String ip = (String) row[1];
            if (!ValidationUtil.isValidIp(ip)) {
                logger.warn("IP ID " + ((Number) row[0]).longValue() + " has an invalid IP \"" + ip + "\".");
                continue;
            }

            retVal.add(new IPResult(
                    ((Number) row[0]).byteValue(),
                    (String) row[1]
            ));
        }

        return retVal;
    }

    public void loadIPs(Set<IPResult> ips, boolean truncate) throws StorageException {
        // TODO: Batch execute
        try {
            if (truncate) {
                sql.execute("PRAGMA foreign_keys = OFF;");
                sql.execute("DELETE FROM `" + prefix + "ips`;");
                sql.execute("VACUUM;");
                longIPIDCache.invalidateAll();
            }
            for (IPResult ip : ips) {
                sql.execute("INSERT INTO `" + prefix + "ips` (`id`, `ip`) VALUES (?, ?);", ip.getLongIPID(), ip.getIP());
                longIPIDCache.put(ip.getIP(), ip.getLongIPID());
            }
            if (truncate) {
                sql.execute("PRAGMA foreign_keys = ON;");
            }
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public Set<PlayerResult> dumpPlayers(long begin, int size) throws StorageException {
        Set<PlayerResult> retVal = new LinkedHashSet<>();

        SQLQueryResult result;
        try {
            result = sql.query("SELECT `id`, `uuid` FROM `" + prefix + "players` LIMIT ?, ?;", begin - 1, size);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }

        for (Object[] row : result.getData()) {
            String pid = (String) row[1];
            if (!ValidationUtil.isValidUuid(pid)) {
                logger.warn("Player ID " + ((Number) row[0]).longValue() + " has an invalid UUID \"" + pid + "\".");
                continue;
            }

            retVal.add(new PlayerResult(
                    ((Number) row[0]).longValue(),
                    UUID.fromString(pid)
            ));
        }

        return retVal;
    }

    public void loadPlayers(Set<PlayerResult> players, boolean truncate) throws StorageException {
        // TODO: Batch execute
        try {
            if (truncate) {
                sql.execute("PRAGMA foreign_keys = OFF;");
                sql.execute("DELETE FROM `" + prefix + "players`;");
                sql.execute("VACUUM;");
                longPlayerIDCache.invalidateAll();
            }
            for (PlayerResult player : players) {
                sql.execute("INSERT INTO `" + prefix + "players` (`id`, `uuid`) VALUES (?, ?);", player.getLongPlayerID(), player.getPlayerID().toString());
                longPlayerIDCache.put(player.getPlayerID(), player.getLongPlayerID());
            }
            if (truncate) {
                sql.execute("PRAGMA foreign_keys = ON;");
            }
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public Set<RawVPNResult> dumpVPNValues(long begin, int size) throws StorageException {
        Set<RawVPNResult> retVal = new LinkedHashSet<>();

        SQLQueryResult result;
        try {
            result = sql.query("SELECT `id`, `ip_id`, `cascade`, `consensus`, `created` FROM `" + prefix + "vpn_values` LIMIT ?, ?;", begin - 1, size);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }

        for (Object[] row : result.getData()) {
            retVal.add(new RawVPNResult(
                    ((Number) row[0]).longValue(),
                    ((Number) row[1]).longValue(),
                    row[2] == null ? Optional.empty() : Optional.of(((Number) row[2]).intValue() == 1),
                    row[3] == null ? Optional.empty() : Optional.of(((Number) row[3]).doubleValue()),
                    getTime(row[4]).getTime()
            ));
        }

        return retVal;
    }

    public void loadVPNValues(Set<RawVPNResult> values, boolean truncate) throws StorageException {
        // TODO: Batch execute
        try {
            if (truncate) {
                sql.execute("PRAGMA foreign_keys = OFF;");
                sql.execute("DELETE FROM `" + prefix + "vpn_values`;");
                sql.execute("VACUUM;");
            }
            for (RawVPNResult value : values) {
                sql.execute("INSERT INTO `" + prefix + "vpn_values` (`id`, `ip_id`, `cascade`, `consensus`, `created`) VALUES (?, ?, ?, ?, ?);", value.getID(), value.getIPID(), value.getCascade().orElse(null), value.getConsensus().orElse(null), new Timestamp(value.getCreated()));
            }
            if (truncate) {
                sql.execute("PRAGMA foreign_keys = ON;");
            }
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public Set<RawMCLeaksResult> dumpMCLeaksValues(long begin, int size) throws StorageException {
        Set<RawMCLeaksResult> retVal = new LinkedHashSet<>();

        SQLQueryResult result;
        try {
            result = sql.query("SELECT `id`, `ip_id`, `cascade`, `consensus`, `created` FROM `" + prefix + "vpn_values` LIMIT ?, ?;", begin - 1, size);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }

        for (Object[] row : result.getData()) {
            retVal.add(new RawMCLeaksResult(
                    ((Number) row[0]).longValue(),
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).intValue() == 1,
                    getTime(row[3]).getTime()
            ));
        }

        return retVal;
    }

    public void loadMCLeaksValues(Set<RawMCLeaksResult> values, boolean truncate) throws StorageException {
        // TODO: Batch execute
        try {
            if (truncate) {
                sql.execute("PRAGMA foreign_keys = OFF;");
                sql.execute("DELETE FROM `" + prefix + "mcleaks_values`;");
                sql.execute("VACUUM;");
            }
            for (RawMCLeaksResult value : values) {
                sql.execute("INSERT INTO `" + prefix + "mcleaks_values` (`id`, `player_id`, `result`, `created`) VALUES (?, ?, ?, ?);", value.getID(), value.getLongPlayerID(), value.getValue(), new Timestamp(value.getCreated()));
            }
            if (truncate) {
                sql.execute("PRAGMA foreign_keys = ON;");
            }
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    private VPNResult getVPNResult(Object[] row) {
        String ip = (String) row[1];
        if (!ValidationUtil.isValidIp(ip)) {
            logger.warn("VPN ID " + row[0] + " has an invalid IP \"" + row[1] + "\".");
            return null;
        }

        return new VPNResult(
                ((Number) row[0]).longValue(),
                ip,
                row[2] == null ? Optional.empty() : Optional.of(((Number) row[2]).intValue() == 1),
                row[3] == null ? Optional.empty() : Optional.of(((Number) row[3]).doubleValue()),
                getTime(row[4]).getTime()
        );
    }

    private MCLeaksResult getMCLeaksResult(Object[] row) {
        String playerID = (String) row[1];
        if (!ValidationUtil.isValidUuid(playerID)) {
            logger.warn("MCLeaks ID " + row[0] + " has an invalid UUID \"" + row[1] + "\".");
            return null;
        }

        return new MCLeaksResult(
                ((Number) row[0]).longValue(),
                UUID.fromString(playerID),
                ((Number) row[2]).intValue() == 1,
                getTime(row[3]).getTime()
        );
    }

    private long getLongIPIDExpensive(String ip) throws SQLException, StorageException {
        // A majority of the time there'll be an ID
        SQLQueryResult result = sql.query("SELECT `id` FROM `" + prefix + "ips` WHERE `ip`=?;", ip);
        if (result.getData().length == 1) {
            return ((Number) result.getData()[0][0]).longValue();
        }

        // No ID, generate one
        SQLExecuteResult r = sql.execute("INSERT INTO `" + prefix + "ips` (`ip`) VALUES (?);", ip);
        if (r.getAutoGeneratedKeys().length != 1) {
            throw new StorageException(false, "Could not get generated keys from inserted IP.");
        }
        long id = ((Number) r.getAutoGeneratedKeys()[0]).longValue();
        handler.ipIDCreationCallback(ip, id, this);
        return id;
    }

    private long getLongPlayerIDExpensive(UUID uuid) throws SQLException, StorageException {
        // A majority of the time there'll be an ID
        SQLQueryResult result = sql.query("SELECT `id` FROM `" + prefix + "players` WHERE `uuid`=?;", uuid.toString());
        if (result.getData().length == 1) {
            return ((Number) result.getData()[0][0]).longValue();
        }

        // No ID, generate one
        SQLExecuteResult r = sql.execute("INSERT INTO `" + prefix + "players` (`uuid`) VALUES (?);", uuid.toString());
        if (r.getAutoGeneratedKeys().length != 1) {
            throw new StorageException(false, "Could not get generated keys from inserted player.");
        }
        long id = ((Number) r.getAutoGeneratedKeys()[0]).longValue();
        handler.playerIDCreationCallback(uuid, id, this);
        return id;
    }

    private Timestamp getTime(Object o) {
        if (o instanceof String) {
            try {
                return Timestamp.valueOf((String) o);
            } catch (IllegalArgumentException ignored) {
                return new Timestamp(Long.valueOf((String) o));
            }
        } else if (o instanceof Number) {
            return new Timestamp(((Number) o).longValue());
        }
        logger.warn("Could not parse time.");
        return new Timestamp(0L);
    }

    protected boolean isAutomaticallyRecoverable(SQLException ex) {
        if (
                ex.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code
                        || ex.getErrorCode() == SQLiteErrorCode.SQLITE_LOCKED.code
                        || ex.getErrorCode() == SQLiteErrorCode.SQLITE_NOMEM.code
                        || ex.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY_RECOVERY.code
                        || ex.getErrorCode() == SQLiteErrorCode.SQLITE_LOCKED_SHAREDCACHE.code
                        || ex.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY_SNAPSHOT.code
                        || ex.getErrorCode() == SQLiteErrorCode.SQLITE_IOERR_NOMEM.code
        ) {
            return true;
        }
        return false;
    }
}

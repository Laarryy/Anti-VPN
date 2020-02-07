package me.egg82.antivpn.storage;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.mysql.cj.exceptions.MysqlErrorNumbers;
import com.zaxxer.hikari.HikariConfig;
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

public class MySQL extends AbstractSQL {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LoadingCache<String, Long> longIPIDCache = Caffeine.newBuilder().build(this::getLongIPIDExpensive);
    private final LoadingCache<UUID, Long> longPlayerIDCache = Caffeine.newBuilder().build(this::getLongPlayerIDExpensive);

    private volatile long lastVPNID;
    private volatile long lastMCLeaksID;
    private StorageHandler handler;

    private MySQL() { }

    private volatile boolean closed = false;

    public void close() {
        closed = true;
        sql.close();
    }

    public boolean isClosed() { return closed || sql.isClosed(); }

    public static MySQL.Builder builder(StorageHandler handler) { return new MySQL.Builder(handler); }

    public static class Builder {
        private final MySQL result = new MySQL();
        private final HikariConfig config = new HikariConfig();

        private Builder(StorageHandler handler) {
            if (handler == null) {
                throw new IllegalArgumentException("handler cannot be null.");
            }

            result.handler = handler;

            // Baseline
            config.setPoolName("AntiVPN-MySQL");
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setConnectionTestQuery("SELECT 1;");
            config.setAutoCommit(true);
            config.addDataSourceProperty("useLegacyDatetimeCode", "false");
            config.addDataSourceProperty("serverTimezone", "UTC");

            // Optimizations
            // http://assets.en.oreilly.com/1/event/21/Connector_J%20Performance%20Gems%20Presentation.pdf
            // https://webcache.googleusercontent.com/search?q=cache:GqZCOIZxeK0J:assets.en.oreilly.com/1/event/21/Connector_J%2520Performance%2520Gems%2520Presentation.pdf+&cd=1&hl=en&ct=clnk&gl=us
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("useLocalTransactionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            config.addDataSourceProperty("useUnbufferedIO", "false");
            config.addDataSourceProperty("useReadAheadInput", "false");
            // https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
        }

        public MySQL.Builder url(String address, int port, String database, String prefix) {
            config.setJdbcUrl("jdbc:mysql://" + address + ":" + port + "/" + database);
            result.database = database;
            result.prefix = prefix;
            return this;
        }

        public MySQL.Builder credentials(String user, String pass) {
            config.setUsername(user);
            config.setPassword(pass);
            return this;
        }

        public MySQL.Builder options(String options) throws IOException {
            options = !options.isEmpty() && options.charAt(0) == '?' ? options.substring(1) : options;
            Properties p = new Properties();
            p.load(new StringReader(options.replace("&", "\n")));
            config.setDataSourceProperties(p);
            return this;
        }

        public MySQL.Builder poolSize(int min, int max) {
            config.setMaximumPoolSize(max);
            config.setMinimumIdle(min);
            return this;
        }

        public MySQL.Builder life(long lifetime, long timeout) {
            config.setMaxLifetime(lifetime);
            config.setConnectionTimeout(timeout);
            return this;
        }

        public MySQL build() throws IOException, StorageException {
            result.sql = new SQL(config);
            SQLVersionUtil.conformVersion(result, "mysql");
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
            result = sql.call("call `" + prefix + "get_vpn_queue_id`(?);", lastVPNID);
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
            result = sql.call("call `" + prefix + "get_mcleaks_queue_id`(?);", lastMCLeaksID);
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
            result = sql.call("call `" + prefix + "get_vpn_ip`(?, ?);", longIPID, cacheTimeMillis);
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
            result = sql.call("call `" + prefix + "get_mcleaks_player`(?, ?);", longPlayerID, cacheTimeMillis);
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
        try {
            sql.execute("INSERT INTO `" + prefix + "vpn_values` (`ip_id`, `cascade`, `consensus`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `cascade`=?, `consensus`=?, `created`=CURRENT_TIMESTAMP();", longIPID, cascade.orElse(null), consensus.orElse(null), cascade.orElse(null), consensus.orElse(null));
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }

        SQLQueryResult query;
        try {
            query = sql.query("SELECT `id`, `created` FROM `" + prefix + "vpn_values` WHERE `ip_id`=?;", longIPID);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        if (query.getData().length != 1) {
            throw new StorageException(false, "Could not get data from inserted value.");
        }

        return new PostVPNResult(
                ((Number) query.getData()[0][0]).longValue(),
                longIPID,
                ip,
                cascade,
                consensus,
                ((Timestamp) query.getData()[0][1]).getTime()
        );
    }

    public PostMCLeaksResult postMCLeaks(UUID playerID, boolean value) throws StorageException {
        if (playerID == null) {
            throw new IllegalArgumentException("playerID cannot be null.");
        }

        long longPlayerID = longPlayerIDCache.get(playerID);
        try {
            sql.execute("INSERT INTO `" + prefix + "mcleaks_values` (`player_id`, `result`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `result`=?, `created`=CURRENT_TIMESTAMP();", longPlayerID, value, value);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }

        SQLQueryResult query;
        try {
            query = sql.query("SELECT `id`, `created` FROM `" + prefix + "mcleaks_values` WHERE `player_id`=?;", longPlayerID);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        if (query.getData().length != 1) {
            throw new StorageException(false, "Could not get data from inserted value.");
        }

        return new PostMCLeaksResult(
                ((Number) query.getData()[0][0]).longValue(),
                longPlayerID,
                playerID,
                value,
                ((Timestamp) query.getData()[0][1]).getTime()
        );
    }

    public void setIPRaw(long longIPID, String ip) throws StorageException {
        try {
            sql.execute("INSERT INTO `" + prefix + "ips` (`id`, `ip`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `ip`=?, `uuid`=?;", longIPID, ip, longIPID, ip);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        longIPIDCache.put(ip, longIPID);
    }

    public void setPlayerRaw(long longPlayerID, UUID playerID) throws StorageException {
        try {
            sql.execute("INSERT INTO `" + prefix + "players` (`id`, `uuid`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `id`=?, `uuid`=?;", longPlayerID, playerID.toString(), longPlayerID, playerID.toString());
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
        longPlayerIDCache.put(playerID, longPlayerID);
    }

    public void postVPNRaw(long id, long longIPID, Optional<Boolean> cascade, Optional<Double> consensus, long created) throws StorageException {
        try {
            sql.execute("INSERT IGNORE INTO `" + prefix + "vpn_values` (`id`, `ip_id`, `cascade`, `consensus`, `created`) VALUES (?, ?, ?, ?, ?);", id, longIPID, cascade.orElse(null), consensus.orElse(null), new Timestamp(created));
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public void postMCLeaksRaw(long id, long longPlayerID, boolean value, long created) throws StorageException {
        try {
            sql.execute("INSERT IGNORE INTO `" + prefix + "mcleaks_values` (`id`, `player_id`, `result`, `created`) VALUES (?, ?, ?, ?);", id, longPlayerID, value, new Timestamp(created));
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    protected void setKey(String key, String value) throws SQLException { sql.execute("INSERT INTO `" + prefix + "data` (`key`, `value`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `value`=?;", key, value, value); }

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
                sql.execute("SET FOREIGN_KEY_CHECKS = 0;");
                sql.execute("TRUNCATE `" + prefix + "ips`;");
                longIPIDCache.invalidateAll();
            }
            for (IPResult ip : ips) {
                sql.execute("INSERT INTO `" + prefix + "ips` (`id`, `ip`) VALUES (?, ?);", ip.getLongIPID(), ip.getIP());
                longIPIDCache.put(ip.getIP(), ip.getLongIPID());
            }
            if (truncate) {
                sql.execute("SET FOREIGN_KEY_CHECKS = 1;");
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
                sql.execute("SET FOREIGN_KEY_CHECKS = 0;");
                sql.execute("TRUNCATE `" + prefix + "players`;");
                longPlayerIDCache.invalidateAll();
            }
            for (PlayerResult player : players) {
                sql.execute("INSERT INTO `" + prefix + "players` (`id`, `uuid`) VALUES (?, ?);", player.getLongPlayerID(), player.getPlayerID().toString());
                longPlayerIDCache.put(player.getPlayerID(), player.getLongPlayerID());
            }
            if (truncate) {
                sql.execute("SET FOREIGN_KEY_CHECKS = 1;");
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
                    row[2] == null ? Optional.empty() : Optional.of((Boolean) row[2]),
                    row[3] == null ? Optional.empty() : Optional.of(((Number) row[3]).doubleValue()),
                    ((Timestamp) row[4]).getTime()
            ));
        }

        return retVal;
    }

    public void loadVPNValues(Set<RawVPNResult> values, boolean truncate) throws StorageException {
        // TODO: Batch execute
        try {
            if (truncate) {
                sql.execute("SET FOREIGN_KEY_CHECKS = 0;");
                sql.execute("TRUNCATE `" + prefix + "vpn_values`;");
            }
            for (RawVPNResult value : values) {
                sql.execute("INSERT INTO `" + prefix + "vpn_values` (`id`, `ip_id`, `cascade`, `consensus`, `created`) VALUES (?, ?, ?, ?, ?);", value.getID(), value.getIPID(), value.getCascade().orElse(null), value.getConsensus().orElse(null), new Timestamp(value.getCreated()));
            }
            if (truncate) {
                sql.execute("SET FOREIGN_KEY_CHECKS = 1;");
            }
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public Set<RawMCLeaksResult> dumpMCLeaksValues(long begin, int size) throws StorageException {
        Set<RawMCLeaksResult> retVal = new LinkedHashSet<>();

        SQLQueryResult result;
        try {
            result = sql.query("SELECT `id`, `player_id`, `result`, `created` FROM `" + prefix + "mcleaks_values` LIMIT ?, ?;", begin - 1, size);
        } catch (SQLException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }

        for (Object[] row : result.getData()) {
            retVal.add(new RawMCLeaksResult(
                    ((Number) row[0]).longValue(),
                    ((Number) row[1]).longValue(),
                    ((Boolean) row[2]).booleanValue(),
                    ((Timestamp) row[3]).getTime()
            ));
        }

        return retVal;
    }

    public void loadMCLeaksValues(Set<RawMCLeaksResult> values, boolean truncate) throws StorageException {
        // TODO: Batch execute
        try {
            if (truncate) {
                sql.execute("SET FOREIGN_KEY_CHECKS = 0;");
                sql.execute("TRUNCATE `" + prefix + "mcleaks_values`;");
            }
            for (RawMCLeaksResult value : values) {
                sql.execute("INSERT INTO `" + prefix + "mcleaks_values` (`id`, `player_id`, `result`, `created`) VALUES (?, ?, ?, ?);", value.getID(), value.getLongPlayerID(), value.getValue(), new Timestamp(value.getCreated()));
            }
            if (truncate) {
                sql.execute("SET FOREIGN_KEY_CHECKS = 1;");
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
                row[2] == null ? Optional.empty() : Optional.of((Boolean) row[2]),
                row[3] == null ? Optional.empty() : Optional.of(((Number) row[3]).doubleValue()),
                ((Timestamp) row[4]).getTime()
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
                ((Boolean) row[2]).booleanValue(),
                ((Timestamp) row[3]).getTime()
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

    protected boolean isAutomaticallyRecoverable(SQLException ex) {
        if (
                ex.getErrorCode() == MysqlErrorNumbers.ER_LOCK_WAIT_TIMEOUT
                        || ex.getErrorCode() == MysqlErrorNumbers.ER_QUERY_TIMEOUT
                        || ex.getErrorCode() == MysqlErrorNumbers.ER_CON_COUNT_ERROR
                        || ex.getErrorCode() == MysqlErrorNumbers.ER_TOO_MANY_DELAYED_THREADS
                        || ex.getErrorCode() == MysqlErrorNumbers.ER_BINLOG_PURGE_EMFILE
                        || ex.getErrorCode() == MysqlErrorNumbers.ER_TOO_MANY_CONCURRENT_TRXS
                        || ex.getErrorCode() == MysqlErrorNumbers.ER_OUTOFMEMORY
                        || ex.getErrorCode() == MysqlErrorNumbers.ER_OUT_OF_SORTMEMORY
                        || ex.getErrorCode() == MysqlErrorNumbers.ER_CANT_CREATE_THREAD
                        || ex.getErrorCode() == MysqlErrorNumbers.ER_OUT_OF_RESOURCES
                        || ex.getErrorCode() == MysqlErrorNumbers.ER_ENGINE_OUT_OF_MEMORY
        ) {
            return true;
        }
        return false;
    }
}

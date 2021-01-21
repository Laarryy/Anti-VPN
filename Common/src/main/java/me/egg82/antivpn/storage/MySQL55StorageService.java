package me.egg82.antivpn.storage;

import com.zaxxer.hikari.HikariConfig;
import io.ebean.config.dbplatform.mysql.MySql55Platform;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import org.checkerframework.checker.nullness.qual.NonNull;

public class MySQL55StorageService extends AbstractJDBCStorageService {
    private MySQL55StorageService(@NonNull String name) {
        super(name);
    }

    public static Builder builder(@NonNull String name) { return new MySQL55StorageService.Builder(name); }

    public static class Builder {
        private final MySQL55StorageService service;
        private final HikariConfig config = new HikariConfig();

        private Builder(@NonNull String name) {
            service = new MySQL55StorageService(name);

            // Baseline
            config.setPoolName("AntiVPN-MySQL55");
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setConnectionTestQuery("SELECT 1;");
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

        public MySQL55StorageService.Builder url(@NonNull String address, int port, @NonNull String database) {
            config.setJdbcUrl("jdbc:mysql://" + address + ":" + port + "/" + database);
            return this;
        }

        public MySQL55StorageService.Builder credentials(@NonNull String user, @NonNull String pass) {
            config.setUsername(user);
            config.setPassword(pass);
            return this;
        }

        public MySQL55StorageService.Builder options(@NonNull String options) throws IOException {
            options = !options.isEmpty() && options.charAt(0) == '?' ? options.substring(1) : options;
            Properties p = new Properties();
            p.load(new StringReader(options.replace("&", "\n")));
            config.setDataSourceProperties(p);
            return this;
        }

        public MySQL55StorageService.Builder poolSize(int min, int max) {
            config.setMaximumPoolSize(max);
            config.setMinimumIdle(min);
            return this;
        }

        public MySQL55StorageService.Builder life(long lifetime, long timeout) {
            config.setMaxLifetime(lifetime);
            config.setConnectionTimeout(timeout);
            return this;
        }

        public @NonNull MySQL55StorageService build() {
            service.createSource(config, new MySql55Platform(), "mysql55");
            return service;
        }
    }
}

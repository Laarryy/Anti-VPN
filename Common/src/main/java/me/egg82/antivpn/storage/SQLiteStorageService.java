package me.egg82.antivpn.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import org.checkerframework.checker.nullness.qual.NonNull;

public class SQLiteStorageService extends AbstractStorageService {
    private SQLiteStorageService(@NonNull String name) {
        super(name);
    }

    public static SQLiteStorageService.Builder builder(@NonNull String name) { return new SQLiteStorageService.Builder(name); }

    public static class Builder {
        private final SQLiteStorageService service;
        private final HikariConfig config = new HikariConfig();

        private Builder(@NonNull String name) {
            service = new SQLiteStorageService(name);

            // Baseline
            config.setPoolName("AntiVPN-SQLite");
            config.setDriverClassName("org.sqlite.JDBC");
            config.setConnectionTestQuery("SELECT 1;");
            config.setAutoCommit(true);
            config.addDataSourceProperty("useLegacyDatetimeCode", "false");
            config.addDataSourceProperty("serverTimezone", "UTC");
        }

        public SQLiteStorageService.Builder file(@NonNull File file) {
            config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            return this;
        }

        public SQLiteStorageService.Builder options(@NonNull String options) throws IOException {
            options = !options.isEmpty() && options.charAt(0) == '?' ? options.substring(1) : options;
            Properties p = new Properties();
            p.load(new StringReader(options.replace("&", "\n")));
            config.setDataSourceProperties(p);
            return this;
        }

        public SQLiteStorageService.Builder poolSize(int min, int max) {
            config.setMaximumPoolSize(max);
            config.setMinimumIdle(min);
            return this;
        }

        public SQLiteStorageService.Builder life(long lifetime, long timeout) {
            config.setMaxLifetime(lifetime);
            config.setConnectionTimeout(timeout);
            return this;
        }

        public @NonNull SQLiteStorageService build() {
            HikariDataSource source = new HikariDataSource(config);
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setDataSource(source);
            dbConfig.setName(service.name);
            service.connection = DatabaseFactory.createWithContextClassLoader(dbConfig, getClass().getClassLoader());

            return service;
        }
    }
}

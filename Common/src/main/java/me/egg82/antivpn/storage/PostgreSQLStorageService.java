package me.egg82.antivpn.storage;

import com.zaxxer.hikari.HikariConfig;
import io.ebean.config.dbplatform.postgres.Postgres9Platform;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PostgreSQLStorageService extends AbstractJDBCStorageService {
    private PostgreSQLStorageService(@NonNull String name) {
        super(name);
    }

    public static Builder builder(@NonNull String name) { return new PostgreSQLStorageService.Builder(name); }

    public static class Builder {
        private final PostgreSQLStorageService service;
        private final HikariConfig config = new HikariConfig();

        private Builder(@NonNull String name) {
            service = new PostgreSQLStorageService(name);

            // Baseline
            config.setPoolName("AntiVPN-PostgreSQL");
            config.setDriverClassName("org.postgresql.Driver");
            config.setConnectionTestQuery("SELECT 1;");
            config.addDataSourceProperty("useLegacyDatetimeCode", "false");
            config.addDataSourceProperty("serverTimezone", "UTC");
        }

        public PostgreSQLStorageService.Builder url(@NonNull String address, int port, @NonNull String database) {
            config.setJdbcUrl("jdbc:postgresql://" + address + ":" + port + "/" + database);
            return this;
        }

        public PostgreSQLStorageService.Builder credentials(@NonNull String user, @NonNull String pass) {
            config.setUsername(user);
            config.setPassword(pass);
            return this;
        }

        public PostgreSQLStorageService.Builder options(@NonNull String options) throws IOException {
            options = !options.isEmpty() && options.charAt(0) == '?' ? options.substring(1) : options;
            Properties p = new Properties();
            p.load(new StringReader(options.replace("&", "\n")));
            config.setDataSourceProperties(p);
            return this;
        }

        public PostgreSQLStorageService.Builder poolSize(int min, int max) {
            config.setMaximumPoolSize(max);
            config.setMinimumIdle(min);
            return this;
        }

        public PostgreSQLStorageService.Builder life(long lifetime, long timeout) {
            config.setMaxLifetime(lifetime);
            config.setConnectionTimeout(timeout);
            return this;
        }

        public @NonNull PostgreSQLStorageService build() {
            service.createSource(config, new Postgres9Platform(), false, "postgresql");
            return service;
        }
    }
}

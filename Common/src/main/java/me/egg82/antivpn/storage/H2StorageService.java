package me.egg82.antivpn.storage;

import com.zaxxer.hikari.HikariConfig;
import io.ebean.config.dbplatform.h2.H2Platform;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public class H2StorageService extends AbstractJDBCStorageService {
    private H2StorageService(@NotNull String name) {
        super(name);
    }

    public static @NotNull H2StorageService.Builder builder(@NotNull String name) { return new H2StorageService.Builder(name); }

    public static class Builder {
        private final H2StorageService service;
        private final HikariConfig config = new HikariConfig();

        private Builder(@NotNull String name) {
            service = new H2StorageService(name);

            // Baseline
            config.setPoolName("Anti-VPN_H2");
            config.setDriverClassName("org.h2.Driver");
            config.setConnectionTestQuery("SELECT 1;");
            config.addDataSourceProperty("useLegacyDatetimeCode", "false");
            config.addDataSourceProperty("serverTimezone", "UTC");
        }

        public @NotNull H2StorageService.Builder file(@NotNull File file) {
            config.setJdbcUrl("jdbc:h2:" + file.getAbsolutePath());
            return this;
        }

        public @NotNull H2StorageService.Builder options(@NotNull String options) throws IOException {
            options = !options.isEmpty() && options.charAt(0) == '?' ? options.substring(1) : options;
            Properties p = new Properties();
            p.load(new StringReader(options.replace("&", "\n")));
            config.setDataSourceProperties(p);
            return this;
        }

        public @NotNull H2StorageService.Builder poolSize(int min, int max) {
            config.setMaximumPoolSize(max);
            config.setMinimumIdle(min);
            return this;
        }

        public @NotNull H2StorageService.Builder life(long lifetime, long timeout) {
            config.setMaxLifetime(lifetime);
            config.setConnectionTimeout(timeout);
            return this;
        }

        public @NotNull H2StorageService build() {
            service.createSource(config, new H2Platform(), false, "h2");
            return service;
        }
    }
}

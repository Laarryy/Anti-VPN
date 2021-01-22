package me.egg82.antivpn.storage;

import com.zaxxer.hikari.HikariConfig;
import io.ebean.config.dbplatform.h2.H2Platform;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import org.checkerframework.checker.nullness.qual.NonNull;

public class H2StorageService extends AbstractJDBCStorageService {
    private H2StorageService(@NonNull String name) {
        super(name);
    }

    public static H2StorageService.Builder builder(@NonNull String name) { return new H2StorageService.Builder(name); }

    public static class Builder {
        private final H2StorageService service;
        private final HikariConfig config = new HikariConfig();

        private Builder(@NonNull String name) {
            service = new H2StorageService(name);

            // Baseline
            config.setPoolName("AntiVPN-H2");
            config.setDriverClassName("org.h2.Driver");
            config.setConnectionTestQuery("SELECT 1;");
            config.addDataSourceProperty("useLegacyDatetimeCode", "false");
            config.addDataSourceProperty("serverTimezone", "UTC");
        }

        public H2StorageService.Builder file(@NonNull File file) {
            config.setJdbcUrl("jdbc:h2:" + file.getAbsolutePath());
            return this;
        }

        public H2StorageService.Builder options(@NonNull String options) throws IOException {
            options = !options.isEmpty() && options.charAt(0) == '?' ? options.substring(1) : options;
            Properties p = new Properties();
            p.load(new StringReader(options.replace("&", "\n")));
            config.setDataSourceProperties(p);
            return this;
        }

        public H2StorageService.Builder poolSize(int min, int max) {
            config.setMaximumPoolSize(max);
            config.setMinimumIdle(min);
            return this;
        }

        public H2StorageService.Builder life(long lifetime, long timeout) {
            config.setMaxLifetime(lifetime);
            config.setConnectionTimeout(timeout);
            return this;
        }

        public @NonNull H2StorageService build() {
            service.createSource(config, new H2Platform(), false, "h2");
            return service;
        }
    }
}

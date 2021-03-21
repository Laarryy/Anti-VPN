package me.egg82.antivpn;

import me.egg82.antivpn.api.platform.Platform;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.config.ConfigurationFileUtil;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.logging.GELFLoggerUtil;
import me.egg82.antivpn.messaging.ServerIDUtil;
import ninja.egg82.mvn.JarBuilder;
import ninja.egg82.mvn.JarInjector;
import ninja.egg82.mvn.RelocationBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;

public class PaperBootstrap extends JavaPlugin {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AntiVPN concrete;

    public PaperBootstrap() {
        super();
        GELFLoggerUtil.setData(ServerIDUtil.getId(new File(getDataFolder(), "stats-id.txt")), getDescription().getVersion(), Platform.Type.PAPER, Bukkit.getVersion());
    }

    protected PaperBootstrap(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
        GELFLoggerUtil.setData(ServerIDUtil.getId(new File(getDataFolder(), "stats-id.txt")), getDescription().getVersion(), Platform.Type.PAPER, Bukkit.getVersion());
    }

    @Override
    public void onLoad() {
        ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            loadJars(new File(getDataFolder(), "cache"), (URLClassLoader) getClass().getClassLoader());
        } catch (ClassCastException | IOException | ModelBuildingException ex) {
            Thread.currentThread().setContextClassLoader(origClassLoader);
            if (getAllowErrorStats()) {
                GELFLoggerUtil.send(3, ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            throw new RuntimeException("Could not load required dependencies.");
        }

        try {
            concrete = new AntiVPN(this);
            concrete.onLoad();
        } catch (Throwable ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            if (getAllowErrorStats()) {
                GELFLoggerUtil.send(3, ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
            throw ex;
        } finally {
            Thread.currentThread().setContextClassLoader(origClassLoader);
        }
    }

    @Override
    public void onEnable() {
        if (concrete == null) {
            return;
        }

        ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            concrete.onEnable();
        } catch (Throwable ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            if (getAllowErrorStats()) {
                GELFLoggerUtil.send(3, ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
            throw ex;
        } finally {
            Thread.currentThread().setContextClassLoader(origClassLoader);
        }
    }

    @Override
    public void onDisable() {
        if (concrete == null) {
            return;
        }

        ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            concrete.onDisable();
        } catch (Throwable ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            if (getAllowErrorStats()) {
                GELFLoggerUtil.send(3, ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
            throw ex;
        } finally {
            Thread.currentThread().setContextClassLoader(origClassLoader);
        }
    }

    private void loadJars(@NotNull File cacheDir, @NotNull URLClassLoader parentLoader) throws IOException, ModelBuildingException {
        Logger builderLogger = new GELFLogger(LoggerFactory.getLogger(JarBuilder.class));

        JarInjector injector = new JarInjector(cacheDir, new GELFLogger(LoggerFactory.getLogger(JarInjector.class)));

        // Caffeine
        injector.addBuilder(new JarBuilder("com.github.ben-manes.caffeine", "caffeine", "${caffeine.version}", builderLogger)
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        injector.addRelocation(new RelocationBuilder("com{}github{}benmanes{}caffeine", "me.egg82.antivpn.external").build("{}", "."));

        // Zstd
        try {
            Class.forName("com.github.luben.zstd.Zstd");
        } catch (ClassNotFoundException ignored) {
            injector.addBuilder(new JarBuilder("com.github.luben", "zstd-jni", "${zstd.version}", builderLogger)
                                        .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        }

        // IPAddress
        injector.addBuilder(new JarBuilder("com.github.seancfoley", "ipaddress", "${ipaddress.version}", builderLogger)
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        injector.addRelocation(new RelocationBuilder("inet{}ipaddr", "me.egg82.antivpn.external").build("{}", "."));

        // H2
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ignored) {
            injector.addBuilder(new JarBuilder("com.h2database", "h2", "${h2.version}", builderLogger)
                                        .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        }

        // MySQL
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            injector.addBuilder(new JarBuilder("mysql", "mysql-connector-java", "${mysql.version}", builderLogger)
                                        .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        }

        // RabbitMQ
        injector.addBuilder(new JarBuilder(replace("com{}rabbitmq"), "amqp-client", "${rabbitmq.version}", builderLogger)
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        injector.addRelocation(new RelocationBuilder("com{}rabbitmq", "me.egg82.antivpn.external").build("{}", "."));

        // Ebean
        injector.addBuilder(new JarBuilder(replace("io{}ebean"), "ebean-core", "${ebean.version}", builderLogger)
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        injector.addRelocation(new RelocationBuilder("io{}ebean", "me.egg82.antivpn.external").build("{}", "."));
        injector.addRelocation(new RelocationBuilder("io{}ebeaninternal", "me.egg82.antivpn.external").build("{}", "."));
        injector.addRelocation(new RelocationBuilder("io{}ebeanservice", "me.egg82.antivpn.external").build("{}", "."));

        // Javassist
        injector.addBuilder(new JarBuilder("org.javassist", getJavassistPackage(), "${javassist.version}", builderLogger)
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        injector.addRelocation(new RelocationBuilder(getJavassistPackage(), "me.egg82.antivpn.external").build());

        // PostgreSQL
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ignored) {
            injector.addBuilder(new JarBuilder("org.postgresql", "postgresql", "${postgresql.version}", builderLogger)
                                        .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        }

        // SQLite
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
            injector.addBuilder(new JarBuilder("org.xerial", "sqlite-jdbc", "${sqlite.version}", builderLogger)
                                        .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        }

        // Jedis
        injector.addBuilder(new JarBuilder("redis.clients", "jedis", "${jedis.version}", builderLogger)
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        injector.addRelocation(new RelocationBuilder("redis{}clients{}jedis", "me.egg82.antivpn.external").build("{}", "."));

        // Inject
        injector.inject(parentLoader);
    }

    // Prevent Maven from relocating these
    @NotNull
    private String replace(@NotNull String pkg) { return pkg.replace("{}", "."); }

    private @NotNull String getJavassistPackage() { return new String(new byte[] { 'j', 'a', 'v', 'a', 's', 's', 'i', 's', 't' }); }

    private boolean getAllowErrorStats() {
        try {
            return ConfigUtil.getConfig().node("stats", "errors").getBoolean(true);
        } catch (IllegalStateException ignored) {
            try {
                return ConfigurationFileUtil.getAllowErrorStats(getDataFolder());
            } catch (Exception ignored2) {
                return false;
            }
        }
    }
}

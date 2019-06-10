package me.egg82.antivpn;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import javax.xml.xpath.XPathExpressionException;
import me.egg82.antivpn.utils.LogUtil;
import ninja.egg82.maven.Artifact;
import ninja.egg82.maven.Scope;
import ninja.egg82.services.ProxiedURLClassLoader;
import ninja.egg82.utils.InjectUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class BukkitBootstrap extends JavaPlugin {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Object concrete;
    private Class<?> concreteClass;

    private final boolean isBukkit;

    private URLClassLoader proxiedClassLoader;

    public BukkitBootstrap() {
        super();
        isBukkit = Bukkit.getName().equals("Bukkit") || Bukkit.getName().equals("CraftBukkit");
    }

    @Override
    public void onLoad() {
        proxiedClassLoader = new ProxiedURLClassLoader(getClass().getClassLoader());

        try {
            loadJars(new File(getDataFolder(), "external"), proxiedClassLoader);
        } catch (ClassCastException | IOException | IllegalAccessException | InvocationTargetException | URISyntaxException | XPathExpressionException | SAXException ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException("Could not load required deps.");
        }

        try {
            concreteClass = proxiedClassLoader.loadClass("me.egg82.antivpn.AntiVPN");
            concrete = concreteClass.getDeclaredConstructor(Plugin.class).newInstance(this);
            concreteClass.getMethod("onLoad").invoke(concrete);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException("Could not create main class.");
        }
    }

    @Override
    public void onEnable() {
        try {
            concreteClass.getMethod("onEnable").invoke(concrete);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException("Could not invoke onEnable.");
        }

        if (ExternalAPI.getInstance() == null) {
            ExternalAPI.setInstance(proxiedClassLoader);
        }
    }

    @Override
    public void onDisable() {
        try {
            concreteClass.getMethod("onDisable").invoke(concrete);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException("Could not invoke onDisable.");
        }
    }

    private void loadJars(File jarsDir, URLClassLoader classLoader) throws IOException, IllegalAccessException, InvocationTargetException, URISyntaxException, XPathExpressionException, SAXException {
        if (jarsDir.exists() && !jarsDir.isDirectory()) {
            Files.delete(jarsDir.toPath());
        }
        if (!jarsDir.exists()) {
            if (!jarsDir.mkdirs()) {
                throw new IOException("Could not create parent directory structure.");
            }
        }

        File cacheDir = new File(jarsDir, "cache");

        // First

        Artifact guava = Artifact.builder("com.google.guava", "guava", "27.1-jre", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(guava, jarsDir, classLoader, "Google Guava", 1);

        // Same file

        InjectUtil.injectFile(getFile(), classLoader);

        // Local

        Artifact taskchainBukkit = Artifact.builder("co.aikar", "taskchain-bukkit", "3.7.2", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/aikar/")
                .addRepository("https://repo.aikar.co/nexus/content/groups/aikar/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(taskchainBukkit, jarsDir, classLoader, "Taskchain", 1);

        printLatest("ACF");
        Artifact acfPaper = Artifact.builder("co.aikar", "acf-paper", "0.5.0-SNAPSHOT", cacheDir)
                .addDirectJarURL("https://nexus.egg82.me/repository/aikar/{GROUP}/{ARTIFACT}/{VERSION}/{ARTIFACT}-{SNAPSHOT}-shaded.jar")
                .addDirectJarURL("https://repo.aikar.co/nexus/content/groups/aikar/{GROUP}/{ARTIFACT}/{VERSION}/{ARTIFACT}-{SNAPSHOT}-shaded.jar")
                .addRepository("https://nexus.egg82.me/repository/aikar/")
                .addRepository("https://repo.aikar.co/nexus/content/groups/aikar/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(acfPaper, jarsDir, classLoader, "ACF", 0);

        Artifact eventChainBukkit = Artifact.builder("ninja.egg82", "event-chain-bukkit", "1.0.9", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(eventChainBukkit, jarsDir, classLoader, "Event Chain");

        Artifact configurateYaml = Artifact.builder("org.spongepowered", "configurate-yaml", "3.6.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/sponge/")
                .addRepository("https://repo.spongepowered.org/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(configurateYaml, jarsDir, classLoader, "Configurate", 2);

        Artifact spigotUpdater = Artifact.builder("ninja.egg82", "spigot-updater", "1.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(spigotUpdater, jarsDir, classLoader, "Spigot Updater");

        Artifact serviceLocator = Artifact.builder("ninja.egg82", "service-locator", "1.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(serviceLocator, jarsDir, classLoader, "Service Locator");

        Artifact commonsNet = Artifact.builder("commons-net", "commons-net", "3.6", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(commonsNet, jarsDir, classLoader, "Apache Commons (Net)");

        printLatest("SQLite");
        Artifact sqlite = Artifact.builder("org.xerial", "sqlite-jdbc", "latest", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(sqlite, jarsDir, classLoader, "SQLite");

        try {
            DriverManager.registerDriver((Driver) Class.forName("org.sqlite.JDBC", true, classLoader).newInstance());
        } catch (ClassNotFoundException | InstantiationException | SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }

        printLatest("MySQL");
        Artifact mysql = Artifact.builder("mysql", "mysql-connector-java", "latest", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(mysql, jarsDir, classLoader, "MySQL", 1);

        try {
            DriverManager.registerDriver((Driver) Class.forName("com.mysql.jdbc.Driver", true, classLoader).newInstance());
        } catch (ClassNotFoundException | InstantiationException | SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }

        // Global

        Artifact caffeine = Artifact.builder("com.github.ben-manes.caffeine", "caffeine", "2.7.0", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(caffeine, jarsDir, classLoader, "Caffeine");

        Artifact concurrentlinkedhashmap = Artifact.builder("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru", "1.4.2", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(concurrentlinkedhashmap, jarsDir, classLoader, "ConcurrentLinkedHashMap");

        Artifact amqpClient = Artifact.builder("com.rabbitmq", "amqp-client", "5.7.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(amqpClient, jarsDir, classLoader, "RabbitMQ");

        Artifact javassist = Artifact.builder("org.javassist", "javassist", "3.25.0-GA", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(javassist, jarsDir, classLoader, "Javassist");

        Artifact javaxAnnotationApi = Artifact.builder("javax.annotation", "javax.annotation-api", "1.3.2", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(javaxAnnotationApi, jarsDir, classLoader, "Javax Annotations");

        Artifact gameanalyticsApi = Artifact.builder("ninja.egg82", "gameanalytics-api", "1.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(gameanalyticsApi, jarsDir, classLoader, "GameAnalytics API", 1);

        Artifact abstractConfiguration = Artifact.builder("ninja.egg82", "abstract-configuration", "1.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(abstractConfiguration, jarsDir, classLoader, "Abstract Configuration");

        Artifact jsonWeb = Artifact.builder("ninja.egg82", "json-web", "1.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(jsonWeb, jarsDir, classLoader, "JSON Web");

        Artifact reflectionUtils = Artifact.builder("ninja.egg82", "reflection-utils", "1.0.2", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(reflectionUtils, jarsDir, classLoader, "Reflection Utils");

        Artifact easySql = Artifact.builder("ninja.egg82", "easy-sql", "1.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(easySql, jarsDir, classLoader, "EasySQL");

        Artifact collections = Artifact.builder("ninja.egg82", "collections", "1.0.0", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(collections, jarsDir, classLoader, "Collections");

        Artifact commonsValidator = Artifact.builder("commons-validator", "commons-validator", "1.6", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(commonsValidator, jarsDir, classLoader, "Apache Commons Validator", 1);

        Artifact jedis = Artifact.builder("redis.clients", "jedis", "3.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(jedis, jarsDir, classLoader, "Jedis", 1);
    }

    private void printLatest(String friendlyName) {
        log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Checking version of " + ChatColor.WHITE + friendlyName);
    }

    private void injectArtifact(Artifact artifact, File jarsDir, URLClassLoader classLoader, String friendlyName) throws IOException, IllegalAccessException, InvocationTargetException, URISyntaxException, XPathExpressionException, SAXException {
        injectArtifact(artifact, jarsDir, classLoader, friendlyName, 0);
    }

    private void injectArtifact(Artifact artifact, File jarsDir, URLClassLoader classLoader, String friendlyName, int depth) throws IOException, IllegalAccessException, InvocationTargetException, URISyntaxException, XPathExpressionException, SAXException {
        File output = new File(jarsDir, artifact.getGroupId()
                + "-" + artifact.getArtifactId()
                + "-" + artifact.getRealVersion() + ".jar"
        );

        if (friendlyName != null && !artifact.fileExists(output)) {
            log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Downloading " + ChatColor.WHITE + friendlyName);
        }
        artifact.injectJar(output, classLoader);

        if (depth > 0) {
            for (Artifact dependency : artifact.getDependencies()) {
                if (dependency.getScope() == Scope.COMPILE || dependency.getScope() == Scope.RUNTIME) {
                    injectArtifact(dependency, jarsDir, classLoader, null, depth - 1);
                }
            }
        }
    }

    private void log(Level level, String message) {
        getServer().getLogger().log(level, (isBukkit) ? ChatColor.stripColor(message) : message);
    }
}

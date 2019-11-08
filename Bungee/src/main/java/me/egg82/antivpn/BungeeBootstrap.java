package me.egg82.antivpn;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.xml.xpath.XPathExpressionException;
import me.egg82.antivpn.utils.LogUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import ninja.egg82.maven.Artifact;
import ninja.egg82.maven.Scope;
import ninja.egg82.services.ProxiedURLClassLoader;
import ninja.egg82.utils.InjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class BungeeBootstrap extends Plugin {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Object concrete;
    private Class<?> concreteClass;

    private URLClassLoader proxiedClassLoader;
    private final ExecutorService downloadPool = Executors.newWorkStealingPool(Math.max(4, Runtime.getRuntime().availableProcessors()));

    @Override
    public void onLoad() {
        proxiedClassLoader = new ProxiedURLClassLoader(getClass().getClassLoader());

        try {
            loadJars(new File(getDataFolder(), "external"), proxiedClassLoader);
        } catch (ClassCastException | IOException | IllegalAccessException | InvocationTargetException ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException("Could not load required deps.");
        }

        downloadPool.shutdown();
        try {
            if (!downloadPool.awaitTermination(1L, TimeUnit.HOURS)) {
                logger.error("Could not download all dependencies. Please try again later.");
                return;
            }
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }

        try {
            DriverManager.registerDriver((Driver) Class.forName("org.sqlite.JDBC", true, proxiedClassLoader).newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }

        // MySQL is automatically registered
        try {
            Class.forName("com.mysql.cj.jdbc.Driver", true, proxiedClassLoader).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
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

    private void loadJars(File jarsDir, URLClassLoader classLoader) throws IOException, IllegalAccessException, InvocationTargetException {
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

        Artifact.Builder guava = Artifact.builder("com.google.guava", "guava", "28.0-jre", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .addRepository("http://central.maven.org/maven2/");
        buildInjectWait(guava, jarsDir, classLoader, "Google Guava", 1);

        // Same file

        InjectUtil.injectFile(getFile(), classLoader);

        // Local

        printLatest("ACF");
        Artifact.Builder acfBungee = Artifact.builder("co.aikar", "acf-bungee", "0.5.0-SNAPSHOT", cacheDir)
                .addDirectJarURL("https://nexus.egg82.me/repository/aikar/{GROUP}/{ARTIFACT}/{VERSION}/{ARTIFACT}-{SNAPSHOT}-shaded.jar")
                .addDirectJarURL("https://repo.aikar.co/nexus/content/groups/aikar/{GROUP}/{ARTIFACT}/{VERSION}/{ARTIFACT}-{SNAPSHOT}-shaded.jar")
                .addRepository("https://nexus.egg82.me/repository/aikar/")
                .addRepository("https://repo.aikar.co/nexus/content/groups/aikar/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(acfBungee, jarsDir, classLoader, "ACF");

        Artifact.Builder eventChainBungee = Artifact.builder("ninja.egg82", "event-chain-bungee", "1.0.9", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(eventChainBungee, jarsDir, classLoader, "Event Chain");

        Artifact.Builder configurateYaml = Artifact.builder("org.spongepowered", "configurate-yaml", "3.6.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/sponge/")
                .addRepository("https://repo.spongepowered.org/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(configurateYaml, jarsDir, classLoader, "Configurate", 2);

        Artifact.Builder spigotUpdater = Artifact.builder("ninja.egg82", "spigot-updater", "1.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(spigotUpdater, jarsDir, classLoader, "Spigot Updater");

        Artifact.Builder serviceLocator = Artifact.builder("ninja.egg82", "service-locator", "1.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(serviceLocator, jarsDir, classLoader, "Service Locator");

        Artifact.Builder commonsNet = Artifact.builder("commons-net", "commons-net", "3.6", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .addRepository("http://central.maven.org/maven2/");
        buildInject(commonsNet, jarsDir, classLoader, "Apache Commons (Net)");

        Artifact.Builder commonsLang3 = Artifact.builder("org.apache.commons", "commons-lang3", "3.9", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .addRepository("http://central.maven.org/maven2/");
        buildInject(commonsLang3, jarsDir, classLoader, "Apache Commons (Lang 3)");

        printLatest("SQLite");
        Artifact.Builder sqlite = Artifact.builder("org.xerial", "sqlite-jdbc", "latest", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .addRepository("http://central.maven.org/maven2/");
        buildInject(sqlite, jarsDir, classLoader, "SQLite");

        printLatest("MySQL");
        Artifact.Builder mysql = Artifact.builder("mysql", "mysql-connector-java", "latest", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .addRepository("http://central.maven.org/maven2/");
        buildInject(mysql, jarsDir, classLoader, "MySQL", 1);

        // Global

        Artifact.Builder caffeine = Artifact.builder("com.github.ben-manes.caffeine", "caffeine", "2.7.0", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .addRepository("http://central.maven.org/maven2/");
        buildInject(caffeine, jarsDir, classLoader, "Caffeine");

        Artifact.Builder concurrentlinkedhashmap = Artifact.builder("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru", "1.4.2", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .addRepository("http://central.maven.org/maven2/");
        buildInject(concurrentlinkedhashmap, jarsDir, classLoader, "ConcurrentLinkedHashMap");

        Artifact.Builder amqpClient = Artifact.builder("com.rabbitmq", "amqp-client", "5.7.3", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .addRepository("http://central.maven.org/maven2/");
        buildInject(amqpClient, jarsDir, classLoader, "RabbitMQ");

        Artifact.Builder javassist = Artifact.builder("org.javassist", "javassist", "3.25.0-GA", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .addRepository("http://central.maven.org/maven2/");
        buildInject(javassist, jarsDir, classLoader, "Javassist");

        Artifact.Builder javaxAnnotationApi = Artifact.builder("javax.annotation", "javax.annotation-api", "1.3.2", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .addRepository("http://central.maven.org/maven2/");
        buildInject(javaxAnnotationApi, jarsDir, classLoader, "Javax Annotations");

        Artifact.Builder gameanalyticsApi = Artifact.builder("ninja.egg82", "gameanalytics-api", "1.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(gameanalyticsApi, jarsDir, classLoader, "GameAnalytics API", 1);

        Artifact.Builder abstractConfiguration = Artifact.builder("ninja.egg82", "abstract-configuration", "1.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(abstractConfiguration, jarsDir, classLoader, "Abstract Configuration");

        Artifact.Builder jsonWeb = Artifact.builder("ninja.egg82", "json-web", "1.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(jsonWeb, jarsDir, classLoader, "JSON Web");

        Artifact.Builder reflectionUtils = Artifact.builder("ninja.egg82", "reflection-utils", "1.0.4", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(reflectionUtils, jarsDir, classLoader, "Reflection Utils");

        Artifact.Builder easySql = Artifact.builder("ninja.egg82", "easy-sql", "1.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(easySql, jarsDir, classLoader, "EasySQL");

        Artifact.Builder collections = Artifact.builder("ninja.egg82", "collections", "1.0.0", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(collections, jarsDir, classLoader, "Collections");

        Artifact.Builder commonsValidator = Artifact.builder("commons-validator", "commons-validator", "1.6", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .addRepository("http://central.maven.org/maven2/");
        buildInject(commonsValidator, jarsDir, classLoader, "Apache Commons Validator", 1);

        Artifact.Builder jedis = Artifact.builder("redis.clients", "jedis", "3.1.0", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .addRepository("http://central.maven.org/maven2/");
        buildInject(jedis, jarsDir, classLoader, "Jedis", 1);
    }

    private void printLatest(String friendlyName) {
        logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "Checking version of " + ChatColor.WHITE + friendlyName);
    }

    private void buildInject(Artifact.Builder builder, File jarsDir, URLClassLoader classLoader, String friendlyName) {
        buildInject(builder, jarsDir, classLoader, friendlyName, 0);
    }

    private void buildInject(Artifact.Builder builder, File jarsDir, URLClassLoader classLoader, String friendlyName, int depth) {
        downloadPool.submit(() -> buildInjectWait(builder, jarsDir, classLoader, friendlyName, depth));
    }

    private void buildInjectWait(Artifact.Builder builder, File jarsDir, URLClassLoader classLoader, String friendlyName, int depth) {
        try {
            injectArtifact(builder.build(), jarsDir, classLoader, friendlyName, depth);
        } catch (IOException | IllegalAccessException | InvocationTargetException | URISyntaxException | XPathExpressionException | SAXException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void injectArtifact(Artifact artifact, File jarsDir, URLClassLoader classLoader, String friendlyName, int depth) throws IOException, IllegalAccessException, InvocationTargetException, URISyntaxException, XPathExpressionException, SAXException {
        File output = new File(jarsDir, artifact.getGroupId()
                + "-" + artifact.getArtifactId()
                + "-" + artifact.getRealVersion() + ".jar"
        );

        if (friendlyName != null && !artifact.fileExists(output)) {
            logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "Downloading " + ChatColor.WHITE + friendlyName);
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
}

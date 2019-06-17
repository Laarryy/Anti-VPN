package me.egg82.antivpn;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
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
import javax.inject.Inject;
import javax.xml.xpath.XPathExpressionException;
import me.egg82.antivpn.utils.LogUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import ninja.egg82.maven.Artifact;
import ninja.egg82.maven.Scope;
import ninja.egg82.services.ProxiedURLClassLoader;
import ninja.egg82.utils.InjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@Plugin(
        id = "antivpn",
        name = "AntiVPN",
        version = "4.6.27",
        authors = "egg82",
        description = "Get the best; save money on overpriced plugins and block VPN users!",
        dependencies = @Dependency(id = "plan", optional = true)
)
public class VelocityBootstrap {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ProxyServer proxy;
    private PluginDescription description;

    private Object concrete;
    private Class<?> concreteClass;

    private URLClassLoader proxiedClassLoader;
    private final ExecutorService downloadPool = Executors.newWorkStealingPool(Math.max(4, Runtime.getRuntime().availableProcessors()));

    @Inject
    public VelocityBootstrap(ProxyServer proxy, PluginDescription description) {
        this.proxy = proxy;
        this.description = description;

        if (!description.getSource().isPresent()) {
            throw new RuntimeException("Could not get plugin file path.");
        }
        if (!description.getName().isPresent()) {
            throw new RuntimeException("Could not get plugin name.");
        }

        proxiedClassLoader = new ProxiedURLClassLoader(getClass().getClassLoader());

        try {
            loadJars(new File(new File(description.getSource().get().getParent().toFile(), description.getName().get()), "external"), proxiedClassLoader);
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
            concrete = concreteClass.getDeclaredConstructor(Object.class, ProxyServer.class, PluginDescription.class).newInstance(this, proxy, description);
            concreteClass.getMethod("onLoad").invoke(concrete);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException("Could not create main class.");
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onEnable(ProxyInitializeEvent event) {
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

    @Subscribe(order = PostOrder.LATE)
    public void onDisable(ProxyShutdownEvent event) {
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

        Artifact.Builder guava = Artifact.builder("com.google.guava", "guava", "27.1-jre", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(guava, jarsDir, classLoader, "Google Guava", 1);

        // Same file

        InjectUtil.injectFile(description.getSource().get().toFile(), classLoader);

        // Local

        printLatest("ACF");
        Artifact.Builder acfVelocity = Artifact.builder("co.aikar", "acf-velocity", "0.5.0-SNAPSHOT", cacheDir)
                .addDirectJarURL("https://nexus.egg82.me/repository/aikar/{GROUP}/{ARTIFACT}/{VERSION}/{ARTIFACT}-{SNAPSHOT}-shaded.jar")
                .addDirectJarURL("https://repo.aikar.co/nexus/content/groups/aikar/{GROUP}/{ARTIFACT}/{VERSION}/{ARTIFACT}-{SNAPSHOT}-shaded.jar")
                .addRepository("https://nexus.egg82.me/repository/aikar/")
                .addRepository("https://repo.aikar.co/nexus/content/groups/aikar/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(acfVelocity, jarsDir, classLoader, "ACF");

        Artifact.Builder eventChainVelocity = Artifact.builder("ninja.egg82", "event-chain-velocity", "1.0.9", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/egg82/")
                .addRepository("https://www.myget.org/F/egg82-java/maven/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(eventChainVelocity, jarsDir, classLoader, "Event Chain");

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
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(commonsNet, jarsDir, classLoader, "Apache Commons (Net)");

        Artifact.Builder commonsLang3 = Artifact.builder("org.apache.commons", "commons-lang3", "3.9", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(commonsLang3, jarsDir, classLoader, "Apache Commons (Lang 3)");

        printLatest("SQLite");
        Artifact.Builder sqlite = Artifact.builder("org.xerial", "sqlite-jdbc", "latest", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(sqlite, jarsDir, classLoader, "SQLite");

        printLatest("MySQL");
        Artifact.Builder mysql = Artifact.builder("mysql", "mysql-connector-java", "latest", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(mysql, jarsDir, classLoader, "MySQL", 1);

        // Global

        Artifact.Builder caffeine = Artifact.builder("com.github.ben-manes.caffeine", "caffeine", "2.7.0", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(caffeine, jarsDir, classLoader, "Caffeine");

        Artifact.Builder concurrentlinkedhashmap = Artifact.builder("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru", "1.4.2", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(concurrentlinkedhashmap, jarsDir, classLoader, "ConcurrentLinkedHashMap");

        Artifact.Builder amqpClient = Artifact.builder("com.rabbitmq", "amqp-client", "5.7.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(amqpClient, jarsDir, classLoader, "RabbitMQ");

        Artifact.Builder javassist = Artifact.builder("org.javassist", "javassist", "3.25.0-GA", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(javassist, jarsDir, classLoader, "Javassist");

        Artifact.Builder javaxAnnotationApi = Artifact.builder("javax.annotation", "javax.annotation-api", "1.3.2", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
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
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(commonsValidator, jarsDir, classLoader, "Apache Commons Validator", 1);

        Artifact.Builder jedis = Artifact.builder("redis.clients", "jedis", "3.0.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/");
        buildInject(jedis, jarsDir, classLoader, "Jedis", 1);
    }

    private void printLatest(String friendlyName) {
        proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Checking version of ").color(TextColor.YELLOW)).append(TextComponent.of(friendlyName).color(TextColor.WHITE)).build());
    }

    private void buildInject(Artifact.Builder builder, File jarsDir, URLClassLoader classLoader, String friendlyName) {
        buildInject(builder, jarsDir, classLoader, friendlyName, 0);
    }

    private void buildInject(Artifact.Builder builder, File jarsDir, URLClassLoader classLoader, String friendlyName, int depth) {
        downloadPool.submit(() -> {
            try {
                injectArtifact(builder.build(), jarsDir, classLoader, friendlyName, depth);
            } catch (IOException | IllegalAccessException | InvocationTargetException | URISyntaxException | XPathExpressionException | SAXException ex) {
                logger.error(ex.getMessage(), ex);
            }
        });
    }

    private void injectArtifact(Artifact artifact, File jarsDir, URLClassLoader classLoader, String friendlyName, int depth) throws IOException, IllegalAccessException, InvocationTargetException, URISyntaxException, XPathExpressionException, SAXException {
        File output = new File(jarsDir, artifact.getGroupId()
                + "-" + artifact.getArtifactId()
                + "-" + artifact.getRealVersion() + ".jar"
        );

        if (friendlyName != null && !artifact.fileExists(output)) {
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Downloading ").color(TextColor.YELLOW)).append(TextComponent.of(friendlyName).color(TextColor.WHITE)).build());
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

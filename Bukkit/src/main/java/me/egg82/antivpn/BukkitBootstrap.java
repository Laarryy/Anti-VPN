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

        InjectUtil.injectFile(getFile(), classLoader);

        File cacheDir = new File(jarsDir, "cache");

        Artifact taskchainBukkit = Artifact.builder("co.aikar", "taskchain-bukkit", "3.7.2", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/aikar/")
                .addRepository("https://repo.aikar.co/nexus/content/groups/aikar/")
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(taskchainBukkit, jarsDir, classLoader, "Taskchain", 1);

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
        injectArtifact(configurateYaml, jarsDir, classLoader, "Configurate", 1);

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

        Artifact sqlite = Artifact.builder("org.xerial", "sqlite-jdbc", "latest", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(sqlite, jarsDir, classLoader, "SQLite");

        try {
            DriverManager.registerDriver((Driver) Class.forName("org.sqlite.JDBC", true, classLoader).newInstance());
        } catch (ClassNotFoundException | InstantiationException | SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }

        Artifact mysql = Artifact.builder("mysql", "mysql-connector-java", "latest", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(mysql, jarsDir, classLoader, "MySQL", 1);

        try {
            DriverManager.registerDriver((Driver) Class.forName("com.mysql.jdbc.Driver", true, classLoader).newInstance());
        } catch (ClassNotFoundException | InstantiationException | SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }

        Artifact caffeine = Artifact.builder("com.github.ben-manes.caffeine", "caffeine", "2.7.0", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(caffeine, jarsDir, classLoader, "Caffeine");

        Artifact guava = Artifact.builder("com.google.guava", "guava", "27.1-jre", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(guava, jarsDir, classLoader, "Google Guava");

        Artifact errorProneAnnotations = Artifact.builder("com.google.errorprone", "error_prone_annotations", "latest", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(errorProneAnnotations, jarsDir, classLoader, "Error Prone Annotations");

        Artifact j2objcAnnotations = Artifact.builder("com.google.j2objc", "j2objc-annotations", "latest", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(j2objcAnnotations, jarsDir, classLoader, "J2objc Annotations");

        Artifact concurrentlinkedhashmap = Artifact.builder("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru", "latest", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(concurrentlinkedhashmap, jarsDir, classLoader, "ConcurrentLinkedHashMap");

        Artifact amqpClient = Artifact.builder("com.rabbitmq", "amqp-client", "5.7.1", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(amqpClient, jarsDir, classLoader, "RabbitMQ");

        Artifact hikariCP = Artifact.builder("com.zaxxer", "HikariCP", "latest", cacheDir)
                .addRepository("https://nexus.egg82.me/repository/maven-central/")
                .build();
        injectArtifact(hikariCP, jarsDir, classLoader, "HikariCP");

        /*log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "Caffeine");
        JarUtil.loadJar("http://central.maven.org/maven2/com/github/ben-manes/caffeine/caffeine/2.6.2/caffeine-2.6.2.jar",
                new File(jarsFolder, "caffeine-2.6.2.jar"),
                new File(jarsFolder, "caffeine-2.6.2-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("com{}github{}benmanes{}caffeine"), parse(externalPath + "{}com{}github{}benmanes{}caffeine"))));

        log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "RabbitMQ");
        JarUtil.loadJar("http://central.maven.org/maven2/com/rabbitmq/amqp-client/5.5.2/amqp-client-5.5.2.jar",
                new File(jarsFolder, "amqp-client-5.5.2.jar"),
                classLoader);

        log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "HikariCP");
        JarUtil.loadJar("http://central.maven.org/maven2/com/zaxxer/HikariCP/3.3.0/HikariCP-3.3.0.jar",
                new File(jarsFolder, "HikariCP-3.3.0.jar"),
                new File(jarsFolder, "HikariCP-3.3.0-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("com{}zaxxer{}hikari"), parse(externalPath + "{}com{}zaxxer{}hikari"))));

        log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "Redis");
        JarUtil.loadJar("http://central.maven.org/maven2/redis/clients/jedis/3.0.1/jedis-3.0.1.jar",
                new File(jarsFolder, "jedis-3.0.1.jar"),
                new File(jarsFolder, "jedis-3.0.1-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("redis{}clients"), parse(externalPath + "{}redis{}clients"))));

        log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "Javassist");
        JarUtil.loadJar("http://central.maven.org/maven2/org/javassist/javassist/3.24.1-GA/javassist-3.24.1-GA.jar",
                new File(jarsFolder, getJavassistString() + "-3.24.1-GA.jar"),
                new File(jarsFolder, getJavassistString() + "-3.24.1-GA-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse(getJavassistString()), parse(externalPath + "{}" + getJavassistString()))));

        log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "Apache Collections");
        JarUtil.loadJar("http://central.maven.org/maven2/commons-collections/commons-collections/3.2.2/commons-collections-3.2.2.jar",
                new File(jarsFolder, "commons-collections-3.2.2.jar"),
                new File(jarsFolder, "commons-collections-3.2.2-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("org{}apache{}commons{}collections"), parse(externalPath + "{}org{}apache{}commons{}collections"))));

        log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "Apache Net Utils");
        JarUtil.loadJar("http://central.maven.org/maven2/commons-net/commons-net/3.6/commons-net-3.6.jar",
                new File(jarsFolder, "commons-net-3.6.jar"),
                new File(jarsFolder, "commons-net-3.6-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("org{}apache{}commons{}net"), parse(externalPath + "{}org{}apache{}commons{}net"))));

        // 0.9.10 for 1.11 compatibility
        log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "Reflections");
        JarUtil.loadJar("http://central.maven.org/maven2/org/reflections/reflections/0.9.10/reflections-0.9.10.jar",
                new File(jarsFolder, "reflections-0.9.10.jar"),
                new File(jarsFolder, "reflections-0.9.10-relocated.jar"),
                classLoader,
                Arrays.asList(
                        new Relocation(parse("org{}reflections"), parse(externalPath + "{}org{}reflections")),
                        new Relocation(parse(getJavassistString()), parse(externalPath + "{}" + getJavassistString()))
                ));

        try {
            Class.forName("org.sqlite.JDBC", false, classLoader);
        } catch (ClassNotFoundException ignored) {
            log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "SQLite");
            JarUtil.loadJar("http://central.maven.org/maven2/org/xerial/sqlite-jdbc/3.25.2/sqlite-jdbc-3.25.2.jar",
                    new File(jarsFolder, "sqlite-jdbc-3.25.2.jar"),
                    classLoader);
        }

        try {
            DriverManager.getDriver("org.sqlite.JDBC");
        } catch (SQLException ignored) {
            try {
                DriverManager.registerDriver((Driver) Class.forName("org.sqlite.JDBC", true, classLoader).newInstance());
            } catch (ClassNotFoundException | InstantiationException | SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        try {
            Class.forName("com.mysql.jdbc.Driver", false, classLoader);
        } catch (ClassNotFoundException ignored) {
            log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "MySQL");
            JarUtil.loadJar("http://central.maven.org/maven2/mysql/mysql-connector-java/8.0.13/mysql-connector-java-8.0.13.jar",
                    new File(jarsFolder, "mysql-connector-java-8.0.13.jar"),
                    classLoader);
        }

        try {
            DriverManager.getDriver("com.mysql.jdbc.Driver");
        } catch (SQLException ignored) {
            try {
                DriverManager.registerDriver((Driver) Class.forName("com.mysql.jdbc.Driver", true, classLoader).newInstance());
            } catch (ClassNotFoundException | InstantiationException | SQLException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }*/
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
                    log(Level.INFO, "Downloading dependency " + dependency);
                    injectArtifact(dependency, jarsDir, classLoader, null, depth - 1);
                }
            }
        }
    }

    private void log(Level level, String message) {
        getServer().getLogger().log(level, (isBukkit) ? ChatColor.stripColor(message) : message);
    }
}

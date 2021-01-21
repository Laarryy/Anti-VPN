package me.egg82.antivpn;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.xml.xpath.XPathExpressionException;
import me.egg82.antivpn.bukkit.BukkitEnvironmentUtil;
import me.egg82.antivpn.utils.BukkitLogUtil;
import me.lucko.jarrelocator.JarRelocator;
import me.lucko.jarrelocator.Relocation;
import ninja.egg82.maven.Artifact;
import ninja.egg82.maven.Repository;
import ninja.egg82.maven.Scope;
import ninja.egg82.utils.DownloadUtil;
import ninja.egg82.utils.InjectUtil;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class BukkitBootstrap extends JavaPlugin {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AntiVPN concrete;

    private final boolean isBukkit;

    private final ExecutorService downloadPool = Executors.newWorkStealingPool(Math.max(4, Runtime.getRuntime().availableProcessors() / 2));

    public BukkitBootstrap() {
        super();
        isBukkit = BukkitEnvironmentUtil.getEnvironment() == BukkitEnvironmentUtil.Environment.BUKKIT;
    }

    protected BukkitBootstrap(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
        isBukkit = BukkitEnvironmentUtil.getEnvironment() == BukkitEnvironmentUtil.Environment.BUKKIT;
    }

    @Override
    public void onLoad() {
        ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            loadJars(new File(getDataFolder(), "external"), (URLClassLoader) getClass().getClassLoader());
        } catch (ClassCastException | IOException ex) {
            Thread.currentThread().setContextClassLoader(origClassLoader);
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException("Could not load required dependencies.");
        }

        downloadPool.shutdown();
        try {
            if (!downloadPool.awaitTermination(1L, TimeUnit.HOURS)) {
                logger.error("Could not download all dependencies. Please try again later.");
                Thread.currentThread().setContextClassLoader(origClassLoader);
                return;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().setContextClassLoader(origClassLoader);
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }

        try {
            concrete = new AntiVPN(this);
            concrete.onLoad();
        } finally {
            Thread.currentThread().setContextClassLoader(origClassLoader);
        }
    }

    @Override
    public void onEnable() {
        ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            concrete.onEnable();
        } finally {
            Thread.currentThread().setContextClassLoader(origClassLoader);
        }
    }

    @Override
    public void onDisable() {
        ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            concrete.onDisable();
        } finally {
            Thread.currentThread().setContextClassLoader(origClassLoader);
        }
    }

    private void loadJars(@NonNull File jarsDir, @NonNull URLClassLoader parentLoader) throws IOException {
        if (jarsDir.exists() && !jarsDir.isDirectory()) {
            Files.delete(jarsDir.toPath());
        }
        if (!jarsDir.exists()) {
            if (!jarsDir.mkdirs()) {
                throw new IOException("Could not create parent directory structure.");
            }
        }

        File cacheDir = new File(jarsDir, "cache");

        Artifact.Builder checkerframework = Artifact.builder(getCheckerFrameworkPackage(), "checker", "${checkerframework.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(checkerframework, jarsDir, Arrays.asList(
                new Relocation(getAnnotatedJdkPackage(), "me.egg82.antivpn.external." + getAnnotatedJdkPackage()),
                new Relocation(getAnnotatorPackage(), "me.egg82.antivpn.external." + getAnnotatorPackage()),
                new Relocation(getCheckerFrameworkPackage(), "me.egg82.antivpn.external." + getCheckerFrameworkPackage()),
                new Relocation(getJmlSpecsPackage(), "me.egg82.antivpn.external." + getJmlSpecsPackage()),
                new Relocation(getSceneLibPackage(), "me.egg82.antivpn.external." + getSceneLibPackage())
        ), parentLoader, "Checker Framework");

        Artifact.Builder caffeine = Artifact.builder("com.github.ben-manes.caffeine", "caffeine", "${caffeine.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(caffeine, jarsDir, Collections.singletonList(new Relocation(getCaffeinePackage(), "me.egg82.antivpn.external." + getCaffeinePackage())), parentLoader, "Caffeine");

        try {
            Class.forName("com.github.luben.zstd.Zstd");
        } catch (ClassNotFoundException ignored) {
            Artifact.Builder zstd = Artifact.builder("com.github.luben", "zstd-jni", "${zstd.version}", cacheDir)
                    .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
            buildRelocateInject(zstd, jarsDir, Collections.emptyList(), parentLoader, "Zstd");
        }

        Artifact.Builder ipaddr = Artifact.builder("com.github.seancfoley", "ipaddress", "${ipaddress.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(ipaddr, jarsDir, Collections.singletonList(new Relocation(getInetIpaddrPackage(), "me.egg82.antivpn.external." + getInetIpaddrPackage())), parentLoader, "IP Address");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            Artifact.Builder mysql = Artifact.builder("mysql", "mysql-connector-java", "${mysql.version}", cacheDir)
                    .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
            buildRelocateInject(mysql, jarsDir, Collections.emptyList(), parentLoader, "MySQL");
        }

        Artifact.Builder rabbitmq = Artifact.builder(getRabbitMqPackage(), "amqp-client", "${rabbitmq.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(rabbitmq, jarsDir, Collections.singletonList(new Relocation(getRabbitMqPackage(), "me.egg82.antivpn.external." + getRabbitMqPackage())), parentLoader, "RabbitMQ");

        Artifact.Builder ebeanCore = Artifact.builder(getEbeanPackage(), "ebean-core", "${ebean.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(ebeanCore, jarsDir, Arrays.asList(
                new Relocation(getEbeanPackage(), "me.egg82.antivpn.external." + getEbeanPackage()),
                new Relocation(getEbeanInternalPackage(), "me.egg82.antivpn.external." + getEbeanInternalPackage()),
                new Relocation(getEbeanServicePackage(), "me.egg82.antivpn.external." + getEbeanServicePackage())
        ), parentLoader, "Ebean Core");

        Artifact.Builder fastutil = Artifact.builder("it.unimi.dsi", "fastutil", "${fastutil.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(fastutil, jarsDir, Collections.singletonList(new Relocation(getFastUtilPackage(), "me.egg82.antivpn.external." + getFastUtilPackage())), parentLoader, "FastUtil");

        Artifact.Builder mcleaks = Artifact.builder("me.gong", "mcleaks-api", "${mcleaks.version}", cacheDir)
                .addRepository(Repository.builder("https://nexus.wesjd.net/repository/thirdparty/").addProxy("https://nexus.egg82.me/repository/wesjd/").build());
        buildRelocateInject(mcleaks, jarsDir, Arrays.asList(
                new Relocation(getMcLeaksPackage(), "me.egg82.antivpn.external." + getMcLeaksPackage()),
                new Relocation(getOkhttp3Package(), "me.egg82.antivpn.external." + getOkhttp3Package()),
                new Relocation(getOkioPackage(), "me.egg82.antivpn.external." + getOkioPackage())
        ), parentLoader, "MC Leaks API");

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ignored) {
            Artifact.Builder sqlite = Artifact.builder("org.postgresql", "postgresql", "${postgresql.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
            buildRelocateInject(sqlite, jarsDir, Collections.emptyList(), parentLoader, "PostgreSQL");
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
            Artifact.Builder sqlite = Artifact.builder("org.xerial", "sqlite-jdbc", "${sqlite.version}", cacheDir)
                    .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
            buildRelocateInject(sqlite, jarsDir, Collections.emptyList(), parentLoader, "SQLite");
        }

        Artifact.Builder sqlite = Artifact.builder("redis.clients", "jedis", "${jedis.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(sqlite, jarsDir, Collections.singletonList(new Relocation(getJedisPackage(), "me.egg82.antivpn.external." + getJedisPackage())), parentLoader, "Jedis");
    }

    // Prevent Maven from relocating these
    private @NonNull String getAnnotatedJdkPackage() { return new String(new byte[] {'a', 'n', 'n', 'o', 't', 'a', 't', 'e', 'd', '-', 'j', 'd', 'k'}); }

    private @NonNull String getAnnotatorPackage() { return new String(new byte[] {'a', 'n', 'n', 'o', 't', 'a', 't', 'o', 'r'}); }

    private @NonNull String getCheckerFrameworkPackage() { return new String(new byte[] {'o', 'r', 'g', '.', 'c', 'h', 'e', 'c', 'k', 'e', 'r', 'f', 'r', 'a', 'm', 'e', 'w', 'o', 'r', 'k'}); }

    private @NonNull String getJmlSpecsPackage() { return new String(new byte[] {'o', 'r', 'g', '.', 'j', 'm', 'l', 's', 'p', 'e', 'c', 's'}); }

    private @NonNull String getSceneLibPackage() { return new String(new byte[] {'s', 'c', 'e', 'n', 'e', 'l', 'i', 'b'}); }

    private @NonNull String getCaffeinePackage() { return new String(new byte[] {'c', 'o', 'm', '.', 'g', 'i', 't', 'h', 'u', 'b', '.', 'b', 'e', 'n', 'm', 'a', 'n', 'e', 's', '.', 'c', 'a', 'f', 'f', 'e', 'i', 'n', 'e'}); }

    private @NonNull String getInetIpaddrPackage() { return new String(new byte[] {'i', 'n', 'e', 't', '.', 'i', 'p', 'a', 'd', 'd', 'r'}); }

    private @NonNull String getRabbitMqPackage() { return new String(new byte[] {'c', 'o', 'm', '.', 'r', 'a', 'b', 'b', 'i', 't', 'm', 'q'}); }

    private @NonNull String getEbeanPackage() { return new String(new byte[] {'i', 'o', '.', 'e', 'b', 'e', 'a', 'n'}); }

    private @NonNull String getEbeanInternalPackage() { return new String(new byte[] {'i', 'o', '.', 'e', 'b', 'e', 'a', 'n', 'i', 'n', 't', 'e', 'r', 'n', 'a', 'l'}); }

    private @NonNull String getEbeanServicePackage() { return new String(new byte[] {'i', 'o', '.', 'e', 'b', 'e', 'a', 'n', 's', 'e', 'r', 'v', 'i', 'c', 'e'}); }

    private @NonNull String getFastUtilPackage() { return new String(new byte[] {'i', 't', '.', 'u', 'n', 'i', 'm', 'i', '.', 'd', 's', 'i', '.', 'f', 'a', 's', 't', 'u', 't', 'i', 'l'}); }

    private @NonNull String getMcLeaksPackage() { return new String(new byte[] {'m', 'e', '.', 'g', 'o', 'n', 'g', '.', 'm', 'c', 'l', 'e', 'a', 'k', 's'}); }

    private @NonNull String getOkhttp3Package() { return new String(new byte[] {'o', 'k', 'h', 't', 't', 'p'}); }

    private @NonNull String getOkioPackage() { return new String(new byte[] {'o', 'k', 'i', 'o'}); }

    private @NonNull String getJedisPackage() { return new String(new byte[] {'r', 'e', 'd', 'i', 's', '.', 'c', 'l', 'i', 'e', 'n', 't', 's', '.', 'j', 'e', 'd', 'i', 's'}); }

    private void printLatest(@NonNull String friendlyName) {
        log(Level.INFO, BukkitLogUtil.HEADING + ChatColor.YELLOW + "Checking version of " + ChatColor.WHITE + friendlyName);
    }

    private void buildInject(Artifact.Builder builder, @NonNull File jarsDir, @NonNull URLClassLoader classLoader, @NonNull String friendlyName) {
        buildInject(builder, jarsDir, classLoader, friendlyName, 0);
    }

    private void buildInject(Artifact.Builder builder, @NonNull File jarsDir, @NonNull URLClassLoader classLoader, @NonNull String friendlyName, int depth) {
        downloadPool.submit(() -> buildInjectWait(builder, jarsDir, classLoader, friendlyName, depth));
    }

    private void buildInjectWait(Artifact.Builder builder, @NonNull File jarsDir, @NonNull URLClassLoader classLoader, @NonNull String friendlyName, int depth) {
        Exception lastEx;
        try {
            injectArtifact(builder.build(), jarsDir, classLoader, friendlyName, depth, null);
            return;
        } catch (IOException ex) {
            lastEx = ex;
        } catch (IllegalAccessException | InvocationTargetException | URISyntaxException | XPathExpressionException | SAXException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (depth > 0) {
            logger.error(lastEx.getMessage(), lastEx);
            return;
        }

        logger.warn("Failed to download/relocate " + builder.getGroupId() + ":" + builder.getArtifactId() + "-" + builder.getVersion() + ". Searching disk instead.", lastEx);

        try {
            injectArtifact(builder, jarsDir, classLoader, null);
        } catch (IOException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("Could not download/relocate " + builder.getGroupId() + ":" + builder.getArtifactId() + "-" + builder.getVersion() + ", and no on-disk option is available.", lastEx);
        }
    }

    private void buildRelocateInject(Artifact.Builder builder, @NonNull File jarsDir, @NonNull List<Relocation> rules, @NonNull URLClassLoader classLoader, @NonNull String friendlyName) {
        buildRelocateInject(builder, jarsDir, rules, classLoader, friendlyName, 0);
    }

    private void buildRelocateInject(Artifact.Builder builder, @NonNull File jarsDir, @NonNull List<Relocation> rules, @NonNull URLClassLoader classLoader, @NonNull String friendlyName, int depth) {
        downloadPool.submit(() -> buildRelocateInjectWait(builder, jarsDir, rules, classLoader, friendlyName, depth));
    }

    private void buildRelocateInjectWait(Artifact.Builder builder, @NonNull File jarsDir, @NonNull List<Relocation> rules, @NonNull URLClassLoader classLoader, @NonNull String friendlyName, int depth) {
        Exception lastEx;
        try {
            injectArtifact(builder.build(), jarsDir, classLoader, friendlyName, depth, rules);
            return;
        } catch (IOException ex) {
            lastEx = ex;
        } catch (IllegalAccessException | InvocationTargetException | URISyntaxException | XPathExpressionException | SAXException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (depth > 0) {
            logger.error(lastEx.getMessage(), lastEx);
            return;
        }

        logger.warn("Failed to download/relocate " + builder.getGroupId() + ":" + builder.getArtifactId() + "-" + builder.getVersion() + ". Searching disk instead.", lastEx);

        try {
            injectArtifact(builder, jarsDir, classLoader, rules);
        } catch (IOException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("Could not download/relocate " + builder.getGroupId() + ":" + builder.getArtifactId() + "-" + builder.getVersion() + ", and no on-disk option is available.", lastEx);
        }
    }

    private void injectArtifact(@NonNull Artifact artifact, @NonNull File jarsDir, @NonNull URLClassLoader classLoader, String friendlyName, int depth, List<Relocation> rules) throws IOException, IllegalAccessException, InvocationTargetException, URISyntaxException, XPathExpressionException, SAXException {
        File output = new File(jarsDir, artifact.getGroupId()
                + "-" + artifact.getArtifactId()
                + "-" + artifact.getRealVersion() + ".jar"
        );

        if (friendlyName != null && !artifact.fileExists(output)) {
            log(Level.INFO, BukkitLogUtil.HEADING + ChatColor.YELLOW + "Downloading " + ChatColor.WHITE + friendlyName);
        }

        if (rules == null) {
            artifact.injectJar(output, classLoader);
        } else {
            if (!DownloadUtil.hasFile(output)) {
                artifact.downloadJar(output);
            }
            File relocatedOutput = new File(jarsDir, artifact.getGroupId()
                    + "-" + artifact.getArtifactId()
                    + "-" + artifact.getRealVersion() + "-relocated.jar"
            );
            if (!DownloadUtil.hasFile(relocatedOutput)) {
                JarRelocator relocator = new JarRelocator(output, relocatedOutput, rules);
                relocator.run();
            }
            InjectUtil.injectFile(relocatedOutput, classLoader);
        }

        if (depth > 0) {
            for (Artifact dependency : artifact.getDependencies()) {
                if (dependency.getScope() == Scope.COMPILE || dependency.getScope() == Scope.RUNTIME) {
                    injectArtifact(dependency, jarsDir, classLoader, null, depth - 1, rules);
                }
            }
        }
    }

    private void injectArtifact(Artifact.Builder builder, @NonNull File jarsDir, @NonNull URLClassLoader classLoader, List<Relocation> rules) throws IOException, IllegalAccessException, InvocationTargetException {
        File[] files = jarsDir.listFiles();
        if (files == null) {
            throw new IOException();
        }

        long latest = Long.MIN_VALUE;
        File retVal = null;
        for (File file : files) {
            if (file.getName().startsWith(builder.getGroupId() + "-" + builder.getArtifactId()) && file.lastModified() >= latest) {
                latest = file.lastModified();
                retVal = file;
            }
        }

        if (retVal == null) {
            throw new IOException();
        }

        if (rules == null) {
            InjectUtil.injectFile(retVal, classLoader);
        } else {
            File output = new File(jarsDir, retVal.getName().substring(0, retVal.getName().length() - 4) + "-relocated.jar");
            if (!DownloadUtil.hasFile(output)) {
                JarRelocator relocator = new JarRelocator(retVal, output, rules);
                relocator.run();
            }
            InjectUtil.injectFile(output, classLoader);
        }
    }

    private void log(@NonNull Level level, @NonNull String message) {
        getServer().getLogger().log(level, (isBukkit) ? ChatColor.stripColor(message) : message);
    }
}

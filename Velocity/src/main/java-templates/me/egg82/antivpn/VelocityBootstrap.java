package me.egg82.antivpn;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.utils.VelocityLogUtil;
import me.lucko.jarrelocator.JarRelocator;
import me.lucko.jarrelocator.Relocation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import ninja.egg82.maven.Artifact;
import ninja.egg82.maven.Repository;
import ninja.egg82.maven.Scope;
import ninja.egg82.utils.DownloadUtil;
import ninja.egg82.utils.HTTPUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "anti-vpn",
        name = "Anti-VPN",
        version = "${plugin.version}",
        authors = "egg82",
        description = "Get the best; save money on overpriced plugins and block VPN users!",
        dependencies = {
                @Dependency(id = "plan", optional = true),
                @Dependency(id = "placeholderapi", optional = true),
                @Dependency(id = "luckperms", optional = true)
        }
)
public class VelocityBootstrap {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProxyServer proxy;
    private final PluginDescription description;

    private AntiVPN concrete;

    private final ExecutorService downloadPool = Executors.newWorkStealingPool(Math.max(4, Runtime.getRuntime().availableProcessors()));

    @Inject
    public VelocityBootstrap(@NotNull ProxyServer proxy, @NotNull PluginDescription description) {
        this.proxy = proxy;
        this.description = description;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onEarlyInit(@NotNull ProxyInitializeEvent event) {
        if (!description.getSource().isPresent()) {
            throw new RuntimeException("Could not get plugin file path.");
        }
        if (!description.getName().isPresent()) {
            throw new RuntimeException("Could not get plugin name.");
        }

        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            loadJars(new File(new File(description.getSource().get().getParent().toFile(), description.getName().get()), "external"), proxy.getPluginManager());
        } catch (ClassCastException | IOException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            throw new RuntimeException("Could not load required dependencies.");
        }

        downloadPool.shutdown();
        try {
            if (!downloadPool.awaitTermination(1L, TimeUnit.HOURS)) {
                logger.error("Could not download all dependencies. Please try again later.");
                return;
            }
        } catch (InterruptedException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }

        concrete = new AntiVPN(this, proxy, description);
        concrete.onLoad();
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onEnable(@NotNull ProxyInitializeEvent event) {
        concrete.onEnable();
    }

    @Subscribe(order = PostOrder.LATE)
    public void onDisable(@NotNull ProxyShutdownEvent event) {
        concrete.onDisable();
    }

    private void loadJars(@NotNull File jarsDir, @NotNull PluginManager pluginManager) throws IOException {
        if (jarsDir.exists() && !jarsDir.isDirectory()) {
            Files.delete(jarsDir.toPath());
        }
        if (!jarsDir.exists()) {
            if (!jarsDir.mkdirs()) {
                throw new IOException("Could not create parent directory structure.");
            }
        }

        File cacheDir = new File(jarsDir, "cache");

        Artifact.Builder caffeine = Artifact.builder("com.github.ben-manes.caffeine", "caffeine", "${caffeine.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(
                caffeine,
                jarsDir,
                Collections.singletonList(new Relocation(getCaffeinePackage(), "me.egg82.antivpn.external." + getCaffeinePackage())),
                pluginManager,
                "Caffeine"
        );

        try {
            Class.forName("com.github.luben.zstd.Zstd");
        } catch (ClassNotFoundException ignored) {
            Artifact.Builder zstd = Artifact.builder("com.github.luben", "zstd-jni", "${zstd.version}", cacheDir)
                    .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
            buildRelocateInject(zstd, jarsDir, Collections.emptyList(), pluginManager, "Zstd");
        }

        Artifact.Builder ipaddr = Artifact.builder("com.github.seancfoley", "ipaddress", "${ipaddress.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(
                ipaddr,
                jarsDir,
                Collections.singletonList(new Relocation(getInetIpaddrPackage(), "me.egg82.antivpn.external." + getInetIpaddrPackage())),
                pluginManager,
                "IP Address"
        );

        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ignored) {
            Artifact.Builder h2 = Artifact.builder("com.h2database", "h2", "${h2.version}", cacheDir)
                    .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
            buildRelocateInject(h2, jarsDir, Collections.emptyList(), pluginManager, "H2");
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            Artifact.Builder mysql = Artifact.builder("mysql", "mysql-connector-java", "${mysql.version}", cacheDir)
                    .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
            buildRelocateInject(mysql, jarsDir, Collections.emptyList(), pluginManager, "MySQL");
        }

        Artifact.Builder rabbitmq = Artifact.builder(getRabbitMqPackage(), "amqp-client", "${rabbitmq.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(
                rabbitmq,
                jarsDir,
                Collections.singletonList(new Relocation(getRabbitMqPackage(), "me.egg82.antivpn.external." + getRabbitMqPackage())),
                pluginManager,
                "RabbitMQ"
        );

        Artifact.Builder ebeanCore = Artifact.builder(getEbeanPackage(), "ebean-core", "${ebean.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(ebeanCore, jarsDir, Arrays.asList(
                new Relocation(getEbeanPackage(), "me.egg82.antivpn.external." + getEbeanPackage()),
                new Relocation(getEbeanInternalPackage(), "me.egg82.antivpn.external." + getEbeanInternalPackage()),
                new Relocation(getEbeanServicePackage(), "me.egg82.antivpn.external." + getEbeanServicePackage())
        ), pluginManager, "Ebean Core");

        Artifact.Builder fastutil = Artifact.builder("it.unimi.dsi", "fastutil", "${fastutil.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(
                fastutil,
                jarsDir,
                Collections.singletonList(new Relocation(getFastUtilPackage(), "me.egg82.antivpn.external." + getFastUtilPackage())),
                pluginManager,
                "FastUtil"
        );

        Artifact.Builder javassist = Artifact.builder("org.javassist", getJavassistPackage(), "${javassist.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(
                javassist,
                jarsDir,
                Collections.singletonList(new Relocation(getJavassistPackage(), "me.egg82.antivpn.external." + getJavassistPackage())),
                pluginManager,
                "Javassist"
        );

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ignored) {
            Artifact.Builder postgresql = Artifact.builder("org.postgresql", "postgresql", "${postgresql.version}", cacheDir)
                    .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
            buildRelocateInject(postgresql, jarsDir, Collections.emptyList(), pluginManager, "PostgreSQL");
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
            Artifact.Builder sqlite = Artifact.builder("org.xerial", "sqlite-jdbc", "${sqlite.version}", cacheDir)
                    .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
            buildRelocateInject(sqlite, jarsDir, Collections.emptyList(), pluginManager, "SQLite");
        }

        Artifact.Builder redis = Artifact.builder("redis.clients", "jedis", "${jedis.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(
                redis,
                jarsDir,
                Collections.singletonList(new Relocation(getJedisPackage(), "me.egg82.antivpn.external." + getJedisPackage())),
                pluginManager,
                "Jedis"
        );
    }

    // Prevent Maven from relocating these
    private @NotNull String getCaffeinePackage() {
        return new String(new byte[] {
                'c',
                'o',
                'm',
                '.',
                'g',
                'i',
                't',
                'h',
                'u',
                'b',
                '.',
                'b',
                'e',
                'n',
                'm',
                'a',
                'n',
                'e',
                's',
                '.',
                'c',
                'a',
                'f',
                'f',
                'e',
                'i',
                'n',
                'e'
        });
    }

    private @NotNull String getInetIpaddrPackage() { return new String(new byte[] { 'i', 'n', 'e', 't', '.', 'i', 'p', 'a', 'd', 'd', 'r' }); }

    private @NotNull String getRabbitMqPackage() { return new String(new byte[] { 'c', 'o', 'm', '.', 'r', 'a', 'b', 'b', 'i', 't', 'm', 'q' }); }

    private @NotNull String getEbeanPackage() { return new String(new byte[] { 'i', 'o', '.', 'e', 'b', 'e', 'a', 'n' }); }

    private @NotNull String getEbeanInternalPackage() {
        return new String(new byte[] {
                'i',
                'o',
                '.',
                'e',
                'b',
                'e',
                'a',
                'n',
                'i',
                'n',
                't',
                'e',
                'r',
                'n',
                'a',
                'l'
        });
    }

    private @NotNull String getEbeanServicePackage() { return new String(new byte[] { 'i', 'o', '.', 'e', 'b', 'e', 'a', 'n', 's', 'e', 'r', 'v', 'i', 'c', 'e' }); }

    private @NotNull String getFastUtilPackage() {
        return new String(new byte[] {
                'i',
                't',
                '.',
                'u',
                'n',
                'i',
                'm',
                'i',
                '.',
                'd',
                's',
                'i',
                '.',
                'f',
                'a',
                's',
                't',
                'u',
                't',
                'i',
                'l'
        });
    }

    private @NotNull String getJavassistPackage() { return new String(new byte[] { 'j', 'a', 'v', 'a', 's', 's', 'i', 's', 't' }); }

    private @NotNull String getJedisPackage() {
        return new String(new byte[] {
                'r',
                'e',
                'd',
                'i',
                's',
                '.',
                'c',
                'l',
                'i',
                'e',
                'n',
                't',
                's',
                '.',
                'j',
                'e',
                'd',
                'i',
                's'
        });
    }

    private void printLatest(@NotNull String friendlyName) {
        proxy.getConsoleCommandSource().sendMessage(VelocityLogUtil.HEADING
                                                            .append(Component.text("Checking version of ", NamedTextColor.YELLOW))
                                                            .append(Component.text(friendlyName, NamedTextColor.WHITE))
        );
    }

    private void buildInject(Artifact.Builder builder, @NotNull File jarsDir, @NotNull PluginManager pluginManager, @NotNull String friendlyName) {
        buildInject(builder, jarsDir, pluginManager, friendlyName, 0);
    }

    private void buildInject(Artifact.Builder builder, @NotNull File jarsDir, @NotNull PluginManager pluginManager, @NotNull String friendlyName, int depth) {
        downloadPool.submit(() -> buildInjectWait(builder, jarsDir, pluginManager, friendlyName, depth));
    }

    private void buildInjectWait(Artifact.Builder builder, @NotNull File jarsDir, @NotNull PluginManager pluginManager, @NotNull String friendlyName, int depth) {
        Exception lastEx;
        try {
            injectArtifact(builder.build(), jarsDir, pluginManager, friendlyName, depth, null);
            return;
        } catch (IOException ex) {
            lastEx = ex;
        } catch (IllegalAccessException | InvocationTargetException | URISyntaxException | XPathExpressionException | SAXException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            return;
        }

        if (depth > 0) {
            logger.error(lastEx.getMessage(), lastEx);
            return;
        }

        logger.warn(
                "Failed to download/relocate " + builder.getGroupId() + ":" + builder.getArtifactId() + "-" + builder.getVersion() + ". Searching disk instead.",
                lastEx
        );

        try {
            injectArtifact(builder, jarsDir, pluginManager, null);
        } catch (IOException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(
                    "Could not download/relocate " + builder.getGroupId() + ":" + builder.getArtifactId() + "-" + builder.getVersion() + ", and no on-disk option is available.",
                    lastEx
            );
        }
    }

    private void buildRelocateInject(
            Artifact.Builder builder,
            @NotNull File jarsDir,
            @NotNull List<Relocation> rules,
            @NotNull PluginManager pluginManager,
            @NotNull String friendlyName
    ) {
        buildRelocateInject(builder, jarsDir, rules, pluginManager, friendlyName, 0);
    }

    private void buildRelocateInject(
            Artifact.Builder builder,
            @NotNull File jarsDir,
            @NotNull List<Relocation> rules,
            @NotNull PluginManager pluginManager,
            @NotNull String friendlyName,
            int depth
    ) {
        downloadPool.submit(() -> buildRelocateInjectWait(builder, jarsDir, rules, pluginManager, friendlyName, depth));
    }

    private void buildRelocateInjectWait(
            Artifact.Builder builder,
            @NotNull File jarsDir,
            @NotNull List<Relocation> rules,
            @NotNull PluginManager pluginManager,
            @NotNull String friendlyName,
            int depth
    ) {
        Exception lastEx;
        try {
            injectArtifact(builder.build(), jarsDir, pluginManager, friendlyName, depth, rules);
            return;
        } catch (IOException ex) {
            lastEx = ex;
        } catch (IllegalAccessException | InvocationTargetException | URISyntaxException | XPathExpressionException | SAXException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            return;
        }

        if (depth > 0) {
            logger.error(lastEx.getMessage(), lastEx);
            return;
        }

        logger.warn(
                "Failed to download/relocate " + builder.getGroupId() + ":" + builder.getArtifactId() + "-" + builder.getVersion() + ". Searching disk instead.",
                lastEx
        );

        try {
            injectArtifact(builder, jarsDir, pluginManager, rules);
        } catch (IOException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(
                    "Could not download/relocate " + builder.getGroupId() + ":" + builder.getArtifactId() + "-" + builder.getVersion() + ", and no on-disk option is available.",
                    lastEx
            );
        }
    }

    private void injectArtifact(
            @NotNull Artifact artifact,
            @NotNull File jarsDir,
            @NotNull PluginManager pluginManager,
            String friendlyName,
            int depth,
            List<Relocation> rules
    ) throws IOException, URISyntaxException, XPathExpressionException, SAXException {
        File output = new File(jarsDir, artifact.getGroupId()
                + "-" + artifact.getArtifactId()
                + "-" + artifact.getRealVersion() + ".jar"
        );

        if (friendlyName != null && !artifact.fileExists(output)) {
            proxy.getConsoleCommandSource().sendMessage(VelocityLogUtil.HEADING
                                                                .append(Component.text("Downloading ", NamedTextColor.YELLOW))
                                                                .append(Component.text(friendlyName, NamedTextColor.WHITE))
            );
        }

        if (rules == null) {
            pluginManager.addToClasspath(this, DownloadUtil.getOrDownloadFile(output, HTTPUtil.toURLs(artifact.getJarURIs())).toPath());
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
            pluginManager.addToClasspath(this, relocatedOutput.toPath());
        }

        if (depth > 0) {
            for (Artifact dependency : artifact.getDependencies()) {
                if (dependency.getScope() == Scope.COMPILE || dependency.getScope() == Scope.RUNTIME) {
                    injectArtifact(dependency, jarsDir, pluginManager, null, depth - 1, rules);
                }
            }
        }
    }

    private void injectArtifact(
            Artifact.Builder builder,
            @NotNull File jarsDir,
            @NotNull PluginManager pluginManager,
            List<Relocation> rules
    ) throws IOException {
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
            pluginManager.addToClasspath(this, retVal.toPath());
        } else {
            File output = new File(jarsDir, retVal.getName().substring(0, retVal.getName().length() - 4) + "-relocated.jar");
            if (!DownloadUtil.hasFile(output)) {
                JarRelocator relocator = new JarRelocator(retVal, output, rules);
                relocator.run();
            }
            pluginManager.addToClasspath(this, output.toPath());
        }
    }
}

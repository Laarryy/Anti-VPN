package me.egg82.antivpn;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.xml.xpath.XPathExpressionException;
import me.egg82.antivpn.bukkit.BukkitEnvironmentUtil;
import me.egg82.antivpn.utils.LogUtil;
import me.lucko.jarrelocator.JarRelocator;
import me.lucko.jarrelocator.Relocation;
import ninja.egg82.maven.Artifact;
import ninja.egg82.maven.Scope;
import ninja.egg82.utils.DownloadUtil;
import ninja.egg82.utils.InjectUtil;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
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

    protected BukkitBootstrap(@NotNull final JavaPluginLoader loader, @NotNull final PluginDescriptionFile description, @NotNull final File dataFolder, @NotNull final File file) {
        super(loader, description, dataFolder, file);
        isBukkit = BukkitEnvironmentUtil.getEnvironment() == BukkitEnvironmentUtil.Environment.BUKKIT;
    }

    @Override
    public void onLoad() {
        try {
            loadJars(new File(getDataFolder(), "external"), (URLClassLoader) getClass().getClassLoader());
        } catch (ClassCastException | IOException | IllegalAccessException | InvocationTargetException ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException("Could not load required dependencies.");
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

        concrete = new AntiVPN(this);
        concrete.onLoad();
    }

    @Override
    public void onEnable() { concrete.onEnable(); }

    @Override
    public void onDisable() { concrete.onDisable(); }

    private void loadJars(@NonNull File jarsDir, @NonNull URLClassLoader parentLoader) throws IOException, IllegalAccessException, InvocationTargetException {
        if (jarsDir.exists() && !jarsDir.isDirectory()) {
            Files.delete(jarsDir.toPath());
        }
        if (!jarsDir.exists()) {
            if (!jarsDir.mkdirs()) {
                throw new IOException("Could not create parent directory structure.");
            }
        }

        File cacheDir = new File(jarsDir, "cache");

        /*Artifact.Builder caffeine = Artifact.builder("com.github.ben-manes.caffeine", "caffeine", "${caffeine.version}", cacheDir)
                .addRepository(Repository.builder("https://repo1.maven.org/maven2/").addProxy("https://nexus.egg82.me/repository/maven-central/").build());
        buildRelocateInject(caffeine, jarsDir, Collections.singletonList(new Relocation(getCaffeinePackage(), "me.egg82.antivpn.external." + getCaffeinePackage())), parentLoader, "Caffeine");*/
    }

    // Prevent Maven from relocating this
    private @NonNull String getCaffeinePackage() { return new String(new byte[] {'c', 'o', 'm', '.', 'g', 'i', 't', 'h', 'u', 'b', '.', 'b', 'e', 'n', 'm', 'a', 'n', 'e', 's', '.', 'c', 'a', 'f', 'f', 'e', 'i', 'n', 'e'}); }

    private void printLatest(@NonNull String friendlyName) {
        log(Level.INFO, LogUtil.HEADING + ChatColor.YELLOW + "Checking version of " + ChatColor.WHITE + friendlyName);
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
            log(Level.INFO, LogUtil.HEADING + ChatColor.YELLOW + "Downloading " + ChatColor.WHITE + friendlyName);
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

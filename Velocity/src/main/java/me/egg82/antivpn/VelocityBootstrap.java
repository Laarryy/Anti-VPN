package me.egg82.antivpn;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;
import javax.inject.Inject;
import me.egg82.antivpn.utils.JarUtil;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import me.lucko.jarrelocator.Relocation;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.slf4j.LoggerFactory;

@Plugin(id="antivpn", name="AntiVPN", version="3.5.18", description="Get the best; save money on overpriced plugins and block VPN users!", authors="egg82")
public class VelocityBootstrap {
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

    private ProxyServer proxy;
    private Logger pluginLogger;
    private PluginDescription description;

    private AntiVPN concrete;
    private final String externalPath = "me{}egg82{}antivpn{}external";

    @Inject
    public VelocityBootstrap(ProxyServer proxy, Logger pluginLogger, PluginDescription description) {
        this.proxy = proxy;
        this.pluginLogger = pluginLogger;
        this.description = description;

        if (!description.getSource().isPresent()) {
            throw new RuntimeException("Could not get plugin file path.");
        }
        if (!description.getName().isPresent()) {
            throw new RuntimeException("Could not get plugin name.");
        }

        try {
            loadJars(new File(new File(description.getSource().get().getParent().toFile(), description.getName().get()), "external"), (URLClassLoader) getClass().getClassLoader());
        } catch (ClassCastException | IOException | IllegalAccessException | InvocationTargetException ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException("Could not load required deps.");
        }

        concrete = new AntiVPN(this, proxy, pluginLogger, description);
    }

    private void loadJars(File jarsFolder, URLClassLoader classLoader) throws IOException, IllegalAccessException, InvocationTargetException {
        if (jarsFolder.exists() && !jarsFolder.isDirectory()) {
            Files.delete(jarsFolder.toPath());
        }
        if (!jarsFolder.exists()) {
            if (!jarsFolder.mkdirs()) {
                throw new IOException("Could not create parent directory structure.");
            }
        }

        proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Loading dep ").color(TextColor.YELLOW)).append(TextComponent.of("Caffeine").color(TextColor.WHITE)).build());
        JarUtil.loadJar("http://central.maven.org/maven2/com/github/ben-manes/caffeine/caffeine/2.6.2/caffeine-2.6.2.jar",
                new File(jarsFolder, "caffeine-2.6.2.jar"),
                new File(jarsFolder, "caffeine-2.6.2-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("com{}github{}benmanes{}caffeine"), parse(externalPath + "{}com{}github{}benmanes{}caffeine"))));

        proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Loading dep ").color(TextColor.YELLOW)).append(TextComponent.of("RabbitMQ").color(TextColor.WHITE)).build());
        JarUtil.loadJar("http://central.maven.org/maven2/com/rabbitmq/amqp-client/5.5.0/amqp-client-5.5.0.jar",
                new File(jarsFolder, "amqp-client-5.5.0.jar"),
                classLoader);

        proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Loading dep ").color(TextColor.YELLOW)).append(TextComponent.of("HikariCP").color(TextColor.WHITE)).build());
        JarUtil.loadJar("http://central.maven.org/maven2/com/zaxxer/HikariCP/3.2.0/HikariCP-3.2.0.jar",
                new File(jarsFolder, "HikariCP-3.2.0.jar"),
                new File(jarsFolder, "HikariCP-3.2.0-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("com{}zaxxer{}hikari"), parse(externalPath + "{}com{}zaxxer{}hikari"))));

        proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Loading dep ").color(TextColor.YELLOW)).append(TextComponent.of("Redis").color(TextColor.WHITE)).build());
        JarUtil.loadJar("http://central.maven.org/maven2/redis/clients/jedis/2.9.0/jedis-2.9.0.jar",
                new File(jarsFolder, "jedis-2.9.0.jar"),
                new File(jarsFolder, "jedis-2.9.0-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("redis{}clients"), parse(externalPath + "{}redis{}clients"))));

        proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Loading dep ").color(TextColor.YELLOW)).append(TextComponent.of("Javassist").color(TextColor.WHITE)).build());
        JarUtil.loadJar("http://central.maven.org/maven2/org/javassist/javassist/3.23.1-GA/javassist-3.23.1-GA.jar",
                new File(jarsFolder, getJavassistString() + "-3.23.1-GA.jar"),
                classLoader);

        proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Loading dep ").color(TextColor.YELLOW)).append(TextComponent.of("Apache Collections").color(TextColor.WHITE)).build());
        JarUtil.loadJar("http://central.maven.org/maven2/commons-collections/commons-collections/3.2.2/commons-collections-3.2.2.jar",
                new File(jarsFolder, "commons-collections-3.2.2.jar"),
                new File(jarsFolder, "commons-collections-3.2.2-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("org{}apache{}commons{}collections"), parse(externalPath + "{}org{}apache{}commons{}collections"))));

        proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Loading dep ").color(TextColor.YELLOW)).append(TextComponent.of("Apache Net Utils").color(TextColor.WHITE)).build());
        JarUtil.loadJar("http://central.maven.org/maven2/commons-net/commons-net/3.6/commons-net-3.6.jar",
                new File(jarsFolder, "commons-net-3.6.jar"),
                new File(jarsFolder, "commons-net-3.6-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("org{}apache{}commons{}net"), parse(externalPath + "{}org{}apache{}commons{}net"))));

        proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Loading dep ").color(TextColor.YELLOW)).append(TextComponent.of("Apache Lang3").color(TextColor.WHITE)).build());
        JarUtil.loadJar("http://central.maven.org/maven2/org/apache/commons/commons-lang3/3.8.1/commons-lang3-3.8.1.jar",
                new File(jarsFolder, "commons-lang3-3.8.1.jar"),
                new File(jarsFolder, "commons-lang3-3.8.1-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("org{}apache{}commons{}lang3"), parse(externalPath + "{}org{}apache{}commons{}lang3"))));

        try {
            Class.forName("org.reflections.Reflections", false, classLoader);
        } catch (ClassNotFoundException ignored) {
            // 0.9.10 for 1.11 compatibility
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Loading dep ").color(TextColor.YELLOW)).append(TextComponent.of("Reflections").color(TextColor.WHITE)).build());
            JarUtil.loadJar("http://central.maven.org/maven2/org/reflections/reflections/0.9.10/reflections-0.9.10.jar",
                    new File(jarsFolder, "reflections-0.9.10.jar"),
                    classLoader);
        }

        try {
            Class.forName("org.sqlite.JDBC", false, classLoader);
        } catch (ClassNotFoundException ignored) {
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Loading dep ").color(TextColor.YELLOW)).append(TextComponent.of("SQLite").color(TextColor.WHITE)).build());
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
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Loading dep ").color(TextColor.YELLOW)).append(TextComponent.of("MySQL").color(TextColor.WHITE)).build());
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
        }
    }

    // Because Maven's relocate is maybe sometimes a bit too powerful ;)
    private String getJavassistString() {
        return new String(new byte[] {'j', 'a', 'v', 'a', 's', 's', 'i', 's', 't'});
    }

    // Because Maven's relocate is maybe sometimes a bit too powerful ;)
    private String parse(String input) {
        return input.replace("{}", ".");
    }

    private UUID getID() {
        String id;
        try {
            id = readID();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }

        if (id == null || id.isEmpty() || id.equalsIgnoreCase("unnamed") || id.equalsIgnoreCase("unknown") || id.equalsIgnoreCase("default") || !ValidationUtil.isValidUuid(id)) {
            id = UUID.randomUUID().toString();
            try {
                writeID(id);
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        return UUID.fromString(id);
    }

    private String readID() throws IOException {
        File config = new File(description.getSource().get().toAbsolutePath().getParent().getParent().toFile(), "velocity-extra.toml");
        if (config.exists() && config.isDirectory()) {
            Files.delete(config.toPath());
        }
        if (!config.exists()) {
            if (!config.createNewFile()) {
                throw new IOException("Stats file could not be created.");
            }
        }

        try (FileReader reader = new FileReader(config); BufferedReader in = new BufferedReader(reader)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().startsWith("stats =")) {
                    return line.trim().substring(7).trim();
                }
            }
        }

        return null;
    }

    private void writeID(String id) throws IOException {
        File config = new File(description.getSource().get().toAbsolutePath().getParent().getParent().toFile(), "velocity-extra.toml");
        if (config.exists() && config.isDirectory()) {
            Files.delete(config.toPath());
        }
        if (!config.exists()) {
            if (!config.createNewFile()) {
                throw new IOException("Stats file could not be created.");
            }
        }

        boolean written = false;
        StringBuilder builder = new StringBuilder();
        try (FileReader reader = new FileReader(config); BufferedReader in = new BufferedReader(reader)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().startsWith("stats =")) {
                    written = true;
                    builder.append("stats = " + id).append(System.lineSeparator());
                } else {
                    builder.append(line).append(System.lineSeparator());
                }
            }
        }
        if (!written) {
            builder.append("stats = " + id).append(System.lineSeparator());
        }

        try (FileWriter out = new FileWriter(config)) {
            out.write(builder.toString());
        }
    }

    public final InputStream getResourceAsStream(String name) { return getClass().getClassLoader().getResourceAsStream(name); }

    @Subscribe(order = PostOrder.EARLY)
    public void onEnable(ProxyInitializeEvent event) {
        concrete.onLoad();
        concrete.onEnable();
    }

    @Subscribe(order = PostOrder.LATE)
    public void onDisable(ProxyShutdownEvent event) {
        concrete.onDisable();
    }
}

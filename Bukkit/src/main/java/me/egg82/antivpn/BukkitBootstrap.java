package me.egg82.antivpn;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.logging.Level;
import me.egg82.antivpn.utils.JarUtil;
import me.egg82.antivpn.utils.LogUtil;
import me.lucko.jarrelocator.Relocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BukkitBootstrap extends JavaPlugin {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AntiVPN concrete;
    private final String externalPath = "me{}egg82{}antivpn{}external";
    private final boolean isBukkit;

    public BukkitBootstrap() {
        super();
        isBukkit = Bukkit.getName().equals("Bukkit") || Bukkit.getName().equals("CraftBukkit");
    }

    @Override
    public void onLoad() {
        try {
            loadJars(new File(getDataFolder(), "external"), (URLClassLoader) getClass().getClassLoader());
        } catch (ClassCastException | IOException | IllegalAccessException | InvocationTargetException ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException("Could not load required deps.");
        }

        concrete = new AntiVPN(this);

        concrete.onLoad();
    }

    @Override
    public void onEnable() {
        concrete.onEnable();
    }

    @Override
    public void onDisable() {
        concrete.onDisable();
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

        log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "Caffeine");
        JarUtil.loadJar("http://central.maven.org/maven2/com/github/ben-manes/caffeine/caffeine/2.6.2/caffeine-2.6.2.jar",
                new File(jarsFolder, "caffeine-2.6.2.jar"),
                new File(jarsFolder, "caffeine-2.6.2-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("com{}github{}benmanes{}caffeine"), parse(externalPath + "{}com{}github{}benmanes{}caffeine"))));

        log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "RabbitMQ");
        JarUtil.loadJar("http://central.maven.org/maven2/com/rabbitmq/amqp-client/5.5.0/amqp-client-5.5.0.jar",
                new File(jarsFolder, "amqp-client-5.5.0.jar"),
                classLoader);

        log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "HikariCP");
        JarUtil.loadJar("http://central.maven.org/maven2/com/zaxxer/HikariCP/3.2.0/HikariCP-3.2.0.jar",
                new File(jarsFolder, "HikariCP-3.2.0.jar"),
                new File(jarsFolder, "HikariCP-3.2.0-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("com{}zaxxer{}hikari"), parse(externalPath + "{}com{}zaxxer{}hikari"))));

        log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "Redis");
        JarUtil.loadJar("http://central.maven.org/maven2/redis/clients/jedis/2.9.0/jedis-2.9.0.jar",
                new File(jarsFolder, "jedis-2.9.0.jar"),
                new File(jarsFolder, "jedis-2.9.0-relocated.jar"),
                classLoader,
                Collections.singletonList(new Relocation(parse("redis{}clients"), parse(externalPath + "{}redis{}clients"))));

        log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "Javassist");
        JarUtil.loadJar("http://central.maven.org/maven2/org/javassist/javassist/3.23.1-GA/javassist-3.23.1-GA.jar",
                new File(jarsFolder, getJavassistString() + "-3.23.1-GA.jar"),
                classLoader);

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

        try {
            Class.forName("org.reflections.Reflections", false, classLoader);
        } catch (ClassNotFoundException ignored) {
            // 0.9.10 for 1.11 compatibility
            log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "Reflections");
            JarUtil.loadJar("http://central.maven.org/maven2/org/reflections/reflections/0.9.10/reflections-0.9.10.jar",
                    new File(jarsFolder, "reflections-0.9.10.jar"),
                    classLoader);
        }

        try {
            Class.forName("org.sqlite.JDBC", false, classLoader);
        } catch (ClassNotFoundException ignored) {
            log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "SQLite");
            JarUtil.loadJar("http://central.maven.org/maven2/org/xerial/sqlite-jdbc/3.25.2/sqlite-jdbc-3.25.2.jar",
                    new File(jarsFolder, "sqlite-jdbc-3.25.2.jar"),
                    classLoader);
        }

        try {
            Class.forName("com.mysql.jdbc.Driver", false, classLoader);
        } catch (ClassNotFoundException ignored) {
            log(Level.INFO, LogUtil.getHeading() + ChatColor.YELLOW + "Loading dep " + ChatColor.WHITE + "MySQL");
            JarUtil.loadJar("http://central.maven.org/maven2/mysql/mysql-connector-java/8.0.13/mysql-connector-java-8.0.13.jar",
                    new File(jarsFolder, "mysql-connector-java-8.0.13.jar"),
                    classLoader);
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

    private void log(Level level, String message) {
        getServer().getLogger().log(level, (isBukkit) ? ChatColor.stripColor(message) : message);
    }
}

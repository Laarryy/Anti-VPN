package me.egg82.antivpn.utils;

import com.google.common.reflect.TypeToken;
import com.rabbitmq.client.ConnectionFactory;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zaxxer.hikari.HikariConfig;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import me.egg82.antivpn.VelocityBootstrap;
import me.egg82.antivpn.apis.API;
import me.egg82.antivpn.enums.SQLType;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.extended.RabbitMQReceiver;
import me.egg82.antivpn.services.InternalAPI;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.sql.SQL;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import redis.clients.jedis.JedisPool;

public class ConfigurationFileUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationFileUtil.class);

    private ConfigurationFileUtil() {}

    public static void reloadConfig(Object plugin, ProxyServer proxy, PluginDescription description) {
        Configuration config;
        try {
            config = getConfig(plugin, "config.yml", new File(new File(description.getSource().get().getParent().toFile(), description.getName().get()), "config.yml"));
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        boolean debug = config.getNode("debug").getBoolean(false);

        if (debug) {
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Debug ").color(TextColor.YELLOW)).append(TextComponent.of("enabled").color(TextColor.WHITE)).build());
        }

        String sourceCacheTime = config.getNode("sources", "cacheTime").getString("6hours");
        Optional<Long> sourceCacheTimeLong = TimeUtil.getTime(sourceCacheTime);
        Optional<TimeUnit> sourceCacheTimeUnit = TimeUtil.getUnit(sourceCacheTime);
        if (!sourceCacheTimeLong.isPresent()) {
            proxy.getConsoleCommandSource().sendMessage(TextComponent.of(TextColor.YELLOW + "sources.cacheTime is not a valid time pattern. Using default value."));
            sourceCacheTimeLong = Optional.of(6L);
            sourceCacheTimeUnit = Optional.of(TimeUnit.HOURS);
        }
        if (!sourceCacheTimeUnit.isPresent()) {
            proxy.getConsoleCommandSource().sendMessage(TextComponent.of(TextColor.YELLOW + "sources.cacheTime is not a valid time pattern. Using default value."));
            sourceCacheTimeLong = Optional.of(6L);
            sourceCacheTimeUnit = Optional.of(TimeUnit.HOURS);
        }

        if (debug) {
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Source cache time: ").color(TextColor.YELLOW)).append(TextComponent.of(sourceCacheTimeUnit.get().toMillis(sourceCacheTimeLong.get()) + " millis").color(TextColor.WHITE)).build());
        }

        String cacheTime = config.getNode("cacheTime").getString("1minute");
        Optional<Long> cacheTimeLong = TimeUtil.getTime(cacheTime);
        Optional<TimeUnit> cacheTimeUnit = TimeUtil.getUnit(cacheTime);
        if (!cacheTimeLong.isPresent()) {
            proxy.getConsoleCommandSource().sendMessage(TextComponent.of(TextColor.YELLOW + "cacheTime is not a valid time pattern. Using default value."));
            cacheTimeLong = Optional.of(1L);
            cacheTimeUnit = Optional.of(TimeUnit.MINUTES);
        }
        if (!cacheTimeUnit.isPresent()) {
            proxy.getConsoleCommandSource().sendMessage(TextComponent.of(TextColor.YELLOW + "cacheTime is not a valid time pattern. Using default value."));
            cacheTimeLong = Optional.of(1L);
            cacheTimeUnit = Optional.of(TimeUnit.MINUTES);
        }

        if (debug) {
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Memory cache time: ").color(TextColor.YELLOW)).append(TextComponent.of(cacheTimeUnit.get().toMillis(cacheTimeLong.get()) + " millis").color(TextColor.WHITE)).build());
        }

        Set<String> sources;
        try {
            sources = new LinkedHashSet<>(config.getNode("sources", "order").getList(TypeToken.of(String.class)));
        } catch (ObjectMappingException ex) {
            logger.error(ex.getMessage(), ex);
            sources = new LinkedHashSet<>();
        }

        for (Iterator<String> i = sources.iterator(); i.hasNext();) {
            String source = i.next();
            if (!config.getNode("sources", source, "enabled").getBoolean()) {
                if (debug) {
                    proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of(source + " is disabled. Removing.").color(TextColor.DARK_RED)).build());
                }
                i.remove();
                continue;
            }

            Optional<API> api = InternalAPI.getAPI(source);
            if (api.isPresent() && api.get().isKeyRequired() && config.getNode("sources", source, "key").getString("").isEmpty()) {
                if (debug) {
                    proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of(source + " requires a key which was not provided. Removing.").color(TextColor.DARK_RED)).build());
                }
                i.remove();
            }
        }

        if (debug) {
            for (String source : sources) {
                proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Added source: ").color(TextColor.YELLOW)).append(TextComponent.of(source).color(TextColor.WHITE)).build());
            }
        }

        Set<String> ignoredIps;
        try {
            ignoredIps = new HashSet<>(config.getNode("kick", "ignore").getList(TypeToken.of(String.class)));
        } catch (ObjectMappingException ex) {
            logger.error(ex.getMessage(), ex);
            ignoredIps = new HashSet<>();
        }

        if (debug) {
            for (String ip : ignoredIps) {
                proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Ignoring IP: ").color(TextColor.YELLOW)).append(TextComponent.of(ip).color(TextColor.WHITE)).build());
            }
        }

        try {
            destroyServices(ServiceLocator.getOptional(CachedConfigValues.class), ServiceLocator.getOptional(RabbitMQReceiver.class));
        } catch (InstantiationException | IllegalAccessException | IOException | TimeoutException ex) {
            logger.error(ex.getMessage(), ex);
        }

        CachedConfigValues cachedValues = CachedConfigValues.builder()
                .sources(sources)
                .sourceCacheTime(sourceCacheTimeLong.get(), sourceCacheTimeUnit.get())
                .ignoredIps(ignoredIps)
                .cacheTime(cacheTimeLong.get(), cacheTimeUnit.get())
                .debug(debug)
                .threads(config.getNode("threads").getInt(4))
                .redisPool(getRedisPool(proxy, config.getNode("redis")))
                .rabbitConnectionFactory(getRabbitConnectionFactory(proxy, config.getNode("rabbitmq")))
                .sql(getSQL(proxy, description, config.getNode("storage")))
                .sqlType(config.getNode("storage", "method").getString("sqlite"))
                .build();

        ServiceLocator.register(config);
        ServiceLocator.register(cachedValues);

        if (debug) {
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("API threads: ").color(TextColor.YELLOW)).append(TextComponent.of(String.valueOf(cachedValues.getThreads())).color(TextColor.WHITE)).build());
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Using Redis: ").color(TextColor.YELLOW)).append(TextComponent.of(String.valueOf(cachedValues.getRedisPool() != null)).color(TextColor.WHITE)).build());
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Using RabbitMQ: ").color(TextColor.YELLOW)).append(TextComponent.of(String.valueOf(cachedValues.getRabbitConnectionFactory() != null)).color(TextColor.WHITE)).build());
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("SQL type: ").color(TextColor.YELLOW)).append(TextComponent.of(cachedValues.getSQLType().name()).color(TextColor.WHITE)).build());
        }
    }

    public static Configuration getConfig(Object plugin, String resourcePath, File fileOnDisk) throws IOException {
        File parentDir = fileOnDisk.getParentFile();
        if (parentDir.exists() && !parentDir.isDirectory()) {
            Files.delete(parentDir.toPath());
        }
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Could not create parent directory structure.");
            }
        }
        if (fileOnDisk.exists() && fileOnDisk.isDirectory()) {
            Files.delete(fileOnDisk.toPath());
        }

        if (!fileOnDisk.exists()) {
            try (InputStreamReader reader = new InputStreamReader(plugin.getClass().getClassLoader().getResourceAsStream(resourcePath));
                 BufferedReader in = new BufferedReader(reader);
                 FileWriter writer = new FileWriter(fileOnDisk);
                 BufferedWriter out = new BufferedWriter(writer)) {
                String line;
                while ((line = in.readLine()) != null) {
                    out.write(line + System.lineSeparator());
                }
            }
        }

        ConfigurationLoader<ConfigurationNode> loader = YAMLConfigurationLoader.builder().setFlowStyle(DumperOptions.FlowStyle.BLOCK).setIndent(2).setFile(fileOnDisk).build();
        ConfigurationNode root = loader.load(ConfigurationOptions.defaults().setHeader("Comments are gone because update :(. Click here for new config + comments: https://www.spigotmc.org/resources/anti-vpn.58291/"));
        Configuration config = new Configuration(root);
        ConfigurationVersionUtil.conformVersion(loader, config, fileOnDisk);

        return config;
    }

    private static void destroyServices(Optional<CachedConfigValues> cachedConfigValues, Optional<RabbitMQReceiver> rabbitReceiver) throws IOException, TimeoutException {
        if (!cachedConfigValues.isPresent()) {
            return;
        }

        cachedConfigValues.get().getSQL().close();

        if (cachedConfigValues.get().getRedisPool() != null) {
            cachedConfigValues.get().getRedisPool().close();
        }

        if (rabbitReceiver.isPresent()) {
            rabbitReceiver.get().close();
        }
    }

    private static SQL getSQL(ProxyServer proxy, PluginDescription description, ConfigurationNode storageConfigNode) {
        SQLType type = SQLType.getByName(storageConfigNode.getNode("method").getString("sqlite"));
        if (type == SQLType.UNKNOWN) {
            proxy.getConsoleCommandSource().sendMessage(TextComponent.of(TextColor.YELLOW + "storage.method is an unknown value. Using default value."));
            type = SQLType.SQLite;
        }

        HikariConfig hikariConfig = new HikariConfig();
        if (type == SQLType.MySQL) {
            hikariConfig.setJdbcUrl("jdbc:mysql://" + storageConfigNode.getNode("data", "address").getString("127.0.0.1:3306") + "/" + storageConfigNode.getNode("data", "database").getString("avpn"));
            hikariConfig.setConnectionTestQuery("SELECT 1;");
        } else if (type == SQLType.SQLite) {
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + new File(new File(description.getSource().get().getParent().toFile(), description.getName().get()), storageConfigNode.getNode("data", "database").getString("avpn") + ".db").getAbsolutePath());
            hikariConfig.setConnectionTestQuery("SELECT 1;");
        }
        hikariConfig.setUsername(storageConfigNode.getNode("data", "username").getString(""));
        hikariConfig.setPassword(storageConfigNode.getNode("data", "password").getString(""));
        hikariConfig.setMaximumPoolSize(storageConfigNode.getNode("settings", "max-pool-size").getInt(2));
        hikariConfig.setMinimumIdle(storageConfigNode.getNode("settings", "min-idle").getInt(2));
        hikariConfig.setMaxLifetime(storageConfigNode.getNode("settings", "max-lifetime").getLong(1800000L));
        hikariConfig.setConnectionTimeout(storageConfigNode.getNode("settings", "timeout").getLong(5000L));
        hikariConfig.addDataSourceProperty("useUnicode", String.valueOf(storageConfigNode.getNode("settings", "properties", "unicode").getBoolean(true)));
        hikariConfig.addDataSourceProperty("characterEncoding", storageConfigNode.getNode("settings", "properties", "encoding").getString("utf8"));
        hikariConfig.setAutoCommit(true);

        // Optimizations
        if (type == SQLType.MySQL) {
            hikariConfig.addDataSourceProperty("useSSL", String.valueOf(storageConfigNode.getNode("data", "ssl").getBoolean(false)));
            // http://assets.en.oreilly.com/1/event/21/Connector_J%20Performance%20Gems%20Presentation.pdf
            hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
            hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
            hikariConfig.addDataSourceProperty("useLocalTransactionState", "true");
            hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
            hikariConfig.addDataSourceProperty("useUnbufferedIO", "false");
            hikariConfig.addDataSourceProperty("useReadAheadInput", "false");
            // https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
            hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        }

        return new SQL(hikariConfig);
    }

    private static JedisPool getRedisPool(ProxyServer proxy, ConfigurationNode redisConfigNode) {
        if (!redisConfigNode.getNode("enabled").getBoolean(false)) {
            return null;
        }

        String address = redisConfigNode.getNode("address").getString("127.0.0.1:6379");
        int portIndex = address.indexOf(':');
        int port;
        if (portIndex > -1) {
            port = Integer.parseInt(address.substring(portIndex + 1));
            address = address.substring(0, portIndex);
        } else {
            proxy.getConsoleCommandSource().sendMessage(TextComponent.of(TextColor.YELLOW + "redis.address port is an unknown value. Using default value."));
            port = 6379;
        }

        return new JedisPool(address, port);
    }

    private static ConnectionFactory getRabbitConnectionFactory(ProxyServer proxy, ConfigurationNode rabbitConfigNode) {
        if (!rabbitConfigNode.getNode("enabled").getBoolean(false)) {
            return null;
        }

        String address = rabbitConfigNode.getNode("address").getString("127.0.0.1:5672");
        int portIndex = address.indexOf(':');
        int port;
        if (portIndex > -1) {
            port = Integer.parseInt(address.substring(portIndex + 1));
            address = address.substring(0, portIndex);
        } else {
            proxy.getConsoleCommandSource().sendMessage(TextComponent.of(TextColor.YELLOW + "rabbitmq.address port is an unknown value. Using default value."));
            port = 5672;
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(address);
        factory.setPort(port);
        factory.setVirtualHost("/");
        factory.setUsername(rabbitConfigNode.getNode("username").getString("guest"));
        factory.setPassword(rabbitConfigNode.getNode("password").getString("guest"));

        return factory;
    }
}

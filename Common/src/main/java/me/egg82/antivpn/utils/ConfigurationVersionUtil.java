package me.egg82.antivpn.utils;

import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationVersionUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationVersionUtil.class);

    private ConfigurationVersionUtil() {}

    public static void conformVersion(ConfigurationLoader<ConfigurationNode> loader, ConfigurationNode config, File fileOnDisk) throws IOException {
        double oldVersion = config.getNode("version").getDouble(1.0d);

        if (config.getNode("version").getDouble(1.0d) == 1.0d) {
            to20(config);
        }
        if (config.getNode("version").getDouble() == 2.0d) {
            to21(config);
        }
        if (config.getNode("version").getDouble() == 2.1d) {
            to22(config);
        }
        if (config.getNode("version").getDouble() == 2.2d) {
            to23(config);
        }
        if (config.getNode("version").getDouble() == 2.3d) {
            to33(config);
        }
        if (config.getNode("version").getDouble() == 3.3d) {
            to34(config);
        }
        if (config.getNode("version").getDouble() == 3.4d) {
            to35(config);
        }
        if (config.getNode("version").getDouble() == 3.5d) {
            to36(config);
        }
        if (config.getNode("version").getDouble() == 3.6d) {
            to37(config);
        }
        if (config.getNode("version").getDouble() == 3.7d) {
            to38(config);
        }
        if (config.getNode("version").getDouble() == 3.8d) {
            to39(config);
        }
        if (config.getNode("version").getDouble() == 3.9d) {
            to49(config);
        }

        if (config.getNode("version").getDouble() != oldVersion) {
            File backupFile = new File(fileOnDisk.getParent(), fileOnDisk.getName() + ".bak");
            if (backupFile.exists()) {
                java.nio.file.Files.delete(backupFile.toPath());
            }

            Files.copy(fileOnDisk, backupFile);
            loader.save(config);
        }
    }

    private static void to20(ConfigurationNode config) {
        // Rabbit -> Messaging
        boolean rabbitEnabled = config.getNode("rabbit", "enabled").getBoolean();
        String rabbitAddress = config.getNode("rabbit", "address").getString("");
        int rabbitPort = config.getNode("rabbit", "port").getInt(5672);
        String rabbitUser = config.getNode("rabbit", "user").getString("guest");
        String rabbitPass = config.getNode("rabbit", "pass").getString("guest");
        config.removeChild("rabbit");
        config.getNode("messaging", "type").setValue((rabbitEnabled) ? "rabbit" : "bungee");
        config.getNode("messaging", "rabbit", "address").setValue(rabbitAddress);
        config.getNode("messaging", "rabbit", "port").setValue(rabbitPort);
        config.getNode("messaging", "rabbit", "user").setValue(rabbitUser);
        config.getNode("messaging", "rabbit", "pass").setValue(rabbitPass);

        // sources.order String -> List
        String[] order = config.getNode("sources", "order").getString("").split(",\\s?");
        config.getNode("sources", "order").setValue(Arrays.asList(order));

        // Add ignore
        config.getNode("ignore").setValue(Arrays.asList("127.0.0.1", "localhost", "::1"));

        // Remove async
        config.removeChild("async");

        // Version
        config.getNode("version").setValue(2.0d);
    }

    private static void to21(ConfigurationNode config) {
        // Add consensus
        config.getNode("consensus").setValue(-1.0d);

        // Version
        config.getNode("version").setValue(2.1d);
    }

    private static void to22(ConfigurationNode config) {
        // Add stats
        config.getNode("stats", "usage").setValue(Boolean.TRUE);
        config.getNode("stats", "errors").setValue(Boolean.TRUE);

        // Add update
        config.getNode("update", "check").setValue(Boolean.TRUE);
        config.getNode("update", "notify").setValue(Boolean.TRUE);

        // Version
        config.getNode("version").setValue(2.2d);
    }

    private static void to23(ConfigurationNode config) {
        // Add voxprox
        config.getNode("sources", "voxprox", "enabled").setValue(Boolean.FALSE);
        config.getNode("sources", "voxprox", "key").setValue("");

        List<String> sources;
        try {
            sources = new ArrayList<>(config.getNode("sources", "order").getList(TypeToken.of(String.class)));
        } catch (Exception ex) {
            sources = new ArrayList<>();
        }
        if (!sources.contains("voxprox")) {
            sources.add("voxprox");
        }
        config.getNode("sources", "order").setValue(sources);

        // Version
        config.getNode("version").setValue(2.3d);
    }

    private static void to33(ConfigurationNode config) {
        // sql -> storage
        String sqlType = config.getNode("sql", "type").getString("sqlite");
        int sqlThreads = config.getNode("sql", "threads").getInt(2);
        String sqlDatabase;
        if (sqlType.equalsIgnoreCase("sqlite")) {
            sqlDatabase = config.getNode("sql", "sqlite", "file").getString("avpn");
            int dotIndex = sqlDatabase.indexOf('.');
            if (dotIndex > 0) {
                sqlDatabase = sqlDatabase.substring(0, dotIndex);
            }
        } else {
            sqlDatabase = config.getNode("sql", "mysql", "database").getString("avpn");
        }
        String mysqlAddress = config.getNode("sql", "mysql", "address").getString("127.0.0.1");
        int mysqlPort = config.getNode("sql", "mysql", "port").getInt(3306);
        String mysqlUser = config.getNode("sql", "mysql", "user").getString("");
        String mysqlPass = config.getNode("sql", "mysql", "pass").getString("");
        config.removeChild("sql");
        config.getNode("storage", "method").setValue(sqlType);
        config.getNode("storage", "data", "address").setValue(mysqlAddress + ":" + mysqlPort);
        config.getNode("storage", "data", "database").setValue(sqlDatabase);
        config.getNode("storage", "data", "prefix").setValue("antivpn_");
        config.getNode("storage", "data", "username").setValue(mysqlUser);
        config.getNode("storage", "data", "password").setValue(mysqlPass);
        config.getNode("storage", "data", "mongodb", "collection-prefix").setValue("");
        config.getNode("storage", "data", "mongodb", "connection-uri").setValue("");
        config.getNode("storage", "settings", "max-pool-size").setValue(sqlThreads);
        config.getNode("storage", "settings", "min-idle").setValue(sqlThreads);
        config.getNode("storage", "settings", "max-lifetime").setValue(1800000L);
        config.getNode("storage", "settings", "timeout").setValue(5000L);
        config.getNode("storage", "settings", "properties", "unicode").setValue(Boolean.TRUE);
        config.getNode("storage", "settings", "properties", "encoding").setValue("utf8");

        // redis
        String redisAddress = config.getNode("redis", "address").getString("");
        if (redisAddress.isEmpty()) {
            redisAddress = "127.0.0.1";
        }
        int redisPort = config.getNode("redis", "port").getInt(6379);
        String redisPass = config.getNode("redis", "pass").getString("");
        config.getNode("redis").removeChild("port");
        config.getNode("redis").removeChild("pass");
        config.getNode("redis", "address").setValue(redisAddress + ":" + redisPort);
        config.getNode("redis", "password").setValue(redisPass);

        // messaging -> rabbitmq
        String messagingType = config.getNode("messaging", "type").getString("");
        String rabbitAddress = config.getNode("messaging", "rabbit", "address").getString("");
        if (rabbitAddress.isEmpty()) {
            rabbitAddress = "127.0.0.1";
        }
        int rabbitPort = config.getNode("messaging", "rabbit", "port").getInt(5672);
        String rabbitUser = config.getNode("messaging", "rabbit", "user").getString("guest");
        String rabbitPass = config.getNode("messaging", "rabbit", "pass").getString("guest");
        config.removeChild("messaging");
        config.getNode("rabbitmq", "enabled").setValue(messagingType.equalsIgnoreCase("rabbit") || messagingType.equalsIgnoreCase("rabbitmq") ? Boolean.TRUE : Boolean.FALSE);
        config.getNode("rabbitmq", "address").setValue(rabbitAddress + ":" + rabbitPort);
        config.getNode("rabbitmq", "username").setValue(rabbitUser);
        config.getNode("rabbitmq", "password").setValue(rabbitPass);

        // kick -> kick.enabled
        boolean kick = config.getNode("kick").getBoolean(true);
        config.removeChild("kick");
        config.getNode("kick", "enabled").setValue(kick);

        // ignore -> kick.ignore
        List<String> ignore;
        try {
            ignore = config.getNode("ignore").getList(TypeToken.of(String.class));
        } catch (Exception ex) {
            ignore = new ArrayList<>();
        }
        config.removeChild("ignore");
        config.getNode("kick", "ignore").setValue(ignore);

        // kickMessage -> kick.message
        String kickMessage = config.getNode("kickMessage").getString("");
        config.removeChild("kickMessage");
        config.getNode("kick", "message").setValue(kickMessage);

        // consensus -> kick.algorithm
        double consensus = config.getNode("consensus").getDouble();
        config.getNode("kick", "algorithm", "method").setValue(consensus >= 0.0d ? "consensus" : "cascade");
        config.getNode("kick", "algorithm", "min-consensus").setValue(consensus >= 0.0d ? consensus : 0.6d);
        config.removeChild("consensus");

        // Add threads
        config.getNode("threads").setValue(4);

        // Version
        config.getNode("version").setValue(3.3d);
    }

    private static void to34(ConfigurationNode config) {
        // Add storage->data->SSL
        config.getNode("storage", "data", "ssl").setValue(Boolean.FALSE);

        // Version
        config.getNode("version").setValue(3.4d);
    }

    private static void to35(ConfigurationNode config) {
        // Remove IPDetector
        List<String> order;
        try {
            order = new ArrayList<>(config.getNode("sources", "order").getList(TypeToken.of(String.class)));
        } catch (ObjectMappingException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        List<String> removed = new ArrayList<>();
        for (String source : order) {
            if (source.equalsIgnoreCase("ipdetector")) { // sources are case-insensitive when loaded
                removed.add(source);
            }
        }

        order.removeAll(removed);
        config.getNode("sources", "order").setValue(order);

        config.getNode("sources").removeChild("ipdetector");

        // Version
        config.getNode("version").setValue(3.5d);
    }

    private static void to36(ConfigurationNode config) {
        // Add ipwarner
        config.getNode("sources", "ipwarner", "enabled").setValue(Boolean.FALSE);
        config.getNode("sources", "ipwarner", "key").setValue("");

        List<String> sources;
        try {
            sources = new ArrayList<>(config.getNode("sources", "order").getList(TypeToken.of(String.class)));
        } catch (Exception ex) {
            sources = new ArrayList<>();
        }
        if (!sources.contains("ipwarner")) {
            sources.add("ipwarner");
        }
        config.getNode("sources", "order").setValue(sources);

        // Version
        config.getNode("version").setValue(3.6d);
    }

    private static void to37(ConfigurationNode config) {
        // Remove kick->enabled
        boolean kickEnabled = config.getNode("kick", "enabled").getBoolean(true);

        // Rename kick->message to action->kick-message
        String kickMessage = config.getNode("kick", "message").getString("");
        config.getNode("action", "kick-message").setValue(kickEnabled ? kickMessage : "");

        // Rename kick->algorithm to action->algorithm
        String algorithmMethod = config.getNode("kick", "algorithm", "method").getString("");
        double algorithmConsensus = config.getNode("kick", "algorithm", "min-consensus").getDouble(0.6d);

        config.getNode("action", "algorithm", "method").setValue(algorithmMethod);
        config.getNode("action", "algorithm", "min-consensus").setValue(algorithmConsensus);

        // Rename kick->ignore to action->ignore
        List<String> ignore;
        try {
            ignore = new ArrayList<>(config.getNode("kick", "ignore").getList(TypeToken.of(String.class)));
        } catch (Exception ex) {
            ignore = new ArrayList<>();
        }

        config.getNode("action", "ignore").setValue(ignore);

        // Remove kick
        config.removeChild("kick");

        // Add action->command
        config.getNode("action", "command").setValue("");

        // Version
        config.getNode("version").setValue(3.7d);
    }

    private static void to38(ConfigurationNode config) {
        // Remove voxprox
        List<String> order;
        try {
            order = new ArrayList<>(config.getNode("sources", "order").getList(TypeToken.of(String.class)));
        } catch (ObjectMappingException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        List<String> removed = new ArrayList<>();
        for (String source : order) {
            if (source.equalsIgnoreCase("voxprox")) { // sources are case-insensitive when loaded
                removed.add(source);
            }
        }

        order.removeAll(removed);
        config.getNode("sources", "order").setValue(order);

        config.getNode("sources").removeChild("voxprox");

        // Add teoh
        config.getNode("sources", "teoh", "enabled").setValue(Boolean.TRUE);

        // Add iphunter
        config.getNode("sources", "iphunter", "enabled").setValue(Boolean.FALSE);
        config.getNode("sources", "iphunter", "key").setValue("");
        config.getNode("sources", "iphunter", "block").setValue(1);

        List<String> sources;
        try {
            sources = new ArrayList<>(config.getNode("sources", "order").getList(TypeToken.of(String.class)));
        } catch (Exception ex) {
            sources = new ArrayList<>();
        }
        if (!sources.contains("teoh")) {
            sources.add("teoh");
        }
        if (!sources.contains("iphunter")) {
            sources.add("iphunter");
        }
        config.getNode("sources", "order").setValue(sources);

        // Version
        config.getNode("version").setValue(3.8d);
    }

    private static void to39(ConfigurationNode config) {
        // Add ip2proxy
        config.getNode("sources", "ip2proxy", "enabled").setValue(Boolean.TRUE);
        config.getNode("sources", "ip2proxy", "key").setValue("demo");

        List<String> sources;
        try {
            sources = new ArrayList<>(config.getNode("sources", "order").getList(TypeToken.of(String.class)));
        } catch (Exception ex) {
            sources = new ArrayList<>();
        }
        if (!sources.contains("ip2proxy")) {
            sources.add("ip2proxy");
        }
        config.getNode("sources", "order").setValue(sources);

        // Version
        config.getNode("version").setValue(3.9d);
    }

    private static void to49(ConfigurationNode config) {
        // Move storage
        String storageMethod = config.getNode("storage", "method").getString("sqlite");
        String storageAddress = config.getNode("storage", "data", "address").getString("127.0.0.1:3306");
        String storageDatabase = config.getNode("storage", "data", "database").getString("anti_vpn");
        String storagePrefix = config.getNode("storage", "data", "prefix").getString("avpn_");
        String storageUser = config.getNode("storage", "data", "username").getString("");
        String storagePass = config.getNode("storage", "data", "password").getString("");
        boolean storageSSL = config.getNode("storage", "data", "ssl").getBoolean(false);
        int storageMaxPoolSize = config.getNode("storage", "settings", "max-pool-size").getInt(4);
        int storageMinIdle = config.getNode("storage", "settings", "min-idle").getInt(4);
        long storageMaxLifetime = config.getNode("storage", "settings", "max-lifetime").getLong(1800000L);
        long storageTimeout = config.getNode("storage", "settings", "timeout").getLong(5000L);
        boolean storageUnicode = config.getNode("storage", "settings", "unicode").getBoolean(true);
        String storageEncoding = config.getNode("storage", "settings", "encoding").getString("utf8");

        config.removeChild("storage");

        config.getNode("storage", "engines", "mysql", "enabled").setValue(storageMethod.equalsIgnoreCase("mysql"));
        config.getNode("storage", "engines", "mysql", "connection", "address").setValue(storageAddress);
        config.getNode("storage", "engines", "mysql", "connection", "database").setValue(storageDatabase);
        config.getNode("storage", "engines", "mysql", "connection", "prefix").setValue(storagePrefix);
        config.getNode("storage", "engines", "mysql", "connection", "username").setValue(storageUser);
        config.getNode("storage", "engines", "mysql", "connection", "password").setValue(storagePass);
        config.getNode("storage", "engines", "mysql", "connection", "options").setValue("useSSL=" + storageSSL + "&useUnicode=" + storageUnicode + "&characterEncoding=" + storageEncoding);
        config.getNode("storage", "engines", "redis", "enabled").setValue(Boolean.FALSE);
        config.getNode("storage", "engines", "redis", "connection", "address").setValue("127.0.0.1:6379");
        config.getNode("storage", "engines", "redis", "connection", "password").setValue("");
        config.getNode("storage", "engines", "redis", "connection", "prefix").setValue("avpn:");
        config.getNode("storage", "engines", "sqlite", "enabled").setValue(storageMethod.equalsIgnoreCase("sqlite"));
        config.getNode("storage", "engines", "sqlite", "connection", "file").setValue(storageDatabase + ".db");
        config.getNode("storage", "engines", "sqlite", "connection", "prefix").setValue(storagePrefix);
        config.getNode("storage", "engines", "sqlite", "connection", "options").setValue("useUnicode=" + storageUnicode + "&characterEncoding=" + storageEncoding);
        config.getNode("storage", "settings", "max-pool-size").setValue(storageMaxPoolSize);
        config.getNode("storage", "settings", "min-idle").setValue(storageMinIdle);
        config.getNode("storage", "settings", "max-lifetime").setValue(storageMaxLifetime);
        config.getNode("storage", "settings", "timeout").setValue(storageTimeout);
        config.getNode("storage", "order").setValue(Arrays.asList("mysql", "redis", "sqlite"));

        // Move messaging
        boolean redisEnabled = config.getNode("redis", "enabled").getBoolean(false);
        String redisAddress = config.getNode("redis", "address").getString("127.0.0.1:6379");
        String redisPass = config.getNode("redis", "password").getString("");
        boolean rabbitEnabled = config.getNode("rabbitmq", "enabled").getBoolean(false);
        String rabbitAddress = config.getNode("rabbitmq", "address").getString("127.0.0.1:5672");
        String rabbitUser = config.getNode("rabbitmq", "username").getString("guest");
        String rabbitPass = config.getNode("rabbitmq", "password").getString("guest");
        int messagingMaxPoolSize = config.getNode("messaging", "settings", "max-pool-size").getInt(4);
        int messagingMinIdle = config.getNode("messaging", "settings", "min-idle").getInt(4);
        long messagingMaxLifetime = config.getNode("messaging", "settings", "max-lifetime").getLong(1800000L);
        long messagingTimeout = config.getNode("messaging", "settings", "timeout").getLong(5000L);

        config.removeChild("redis");
        config.removeChild("rabbitmq");

        config.getNode("messaging", "engines", "redis", "enabled").setValue(redisEnabled);
        config.getNode("messaging", "engines", "redis", "connection", "address").setValue(redisAddress);
        config.getNode("messaging", "engines", "redis", "connection", "password").setValue(redisPass);
        config.getNode("messaging", "engines", "rabbitmq", "enabled").setValue(rabbitEnabled);
        config.getNode("messaging", "engines", "rabbitmq", "connection", "address").setValue(rabbitAddress);
        config.getNode("messaging", "engines", "rabbitmq", "connection", "v-host").setValue("/");
        config.getNode("messaging", "engines", "rabbitmq", "connection", "username").setValue(rabbitUser);
        config.getNode("messaging", "engines", "rabbitmq", "connection", "password").setValue(rabbitPass);
        config.getNode("messaging", "settings", "max-pool-size").setValue(messagingMaxPoolSize);
        config.getNode("messaging", "settings", "min-idle").setValue(messagingMinIdle);
        config.getNode("messaging", "settings", "max-lifetime").setValue(messagingMaxLifetime);
        config.getNode("messaging", "settings", "timeout").setValue(messagingTimeout);
        config.getNode("messaging", "order").setValue(Arrays.asList("rabbitmq", "redis"));

        // action->command to action->commands
        String actionCommand = config.getNode("action", "command").getString("");
        config.getNode("action").removeChild("command");
        config.getNode("action", "commands").setValue(Collections.singleton(""));

        // cacheTime & threads to connection->cache-time & threads
        String cacheTime = config.getNode("cacheTime").getString("1minute");
        int threads = config.getNode("threads").getInt(4);
        config.removeChild("cacheTime");
        config.removeChild("threads");
        config.getNode("connection", "cache-time").setValue(cacheTime);
        config.getNode("connection", "threads").setValue(threads);

        // sources->cacheTime to sources->cache-time
        String sourcesCacheTime = config.getNode("sources", "cacheTime").getString("6hours");
        config.getNode("sources").removeChild("cacheTime");
        config.getNode("sources", "cache-time").setValue(sourcesCacheTime);

        // Version
        config.getNode("version").setValue(4.9d);
    }
}

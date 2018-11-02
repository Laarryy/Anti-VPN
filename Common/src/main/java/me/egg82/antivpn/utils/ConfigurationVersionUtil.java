package me.egg82.antivpn.utils;

import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

public class ConfigurationVersionUtil {
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

        if (config.getNode("version").getDouble() != oldVersion) {
            File backupFile = new File(fileOnDisk.getParent(), fileOnDisk.getName() + ".bak");
            java.nio.file.Files.delete(backupFile.toPath());

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
            sources = config.getNode("sources", "order").getList(TypeToken.of(String.class));
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
}

package me.egg82.antivpn.config;

import com.google.common.io.Files;
import me.egg82.antivpn.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ConfigurationVersionUtil {
    private ConfigurationVersionUtil() { }

    public static void conformVersion(
            @NotNull ConfigurationLoader<CommentedConfigurationNode> loader,
            @NotNull CommentedConfigurationNode config,
            @NotNull File fileOnDisk
    ) throws IOException {
        double oldVersion = config.node("version").getDouble(1.0d);

        if (config.node("version").getDouble(1.0d) == 1.0d) {
            to20(config);
        }
        if (config.node("version").getDouble() == 2.0d) {
            to21(config);
        }
        if (config.node("version").getDouble() == 2.1d) {
            to22(config);
        }
        if (config.node("version").getDouble() == 2.2d) {
            to23(config);
        }
        if (config.node("version").getDouble() == 2.3d) {
            to33(config);
        }
        if (config.node("version").getDouble() == 3.3d) {
            to34(config);
        }
        if (config.node("version").getDouble() == 3.4d) {
            to35(config);
        }
        if (config.node("version").getDouble() == 3.5d) {
            to36(config);
        }
        if (config.node("version").getDouble() == 3.6d) {
            to37(config);
        }
        if (config.node("version").getDouble() == 3.7d) {
            to38(config);
        }
        if (config.node("version").getDouble() == 3.8d) {
            to39(config);
        }
        if (config.node("version").getDouble() == 3.9d) {
            to49(config);
        }
        if (config.node("version").getDouble() == 4.9d) {
            to411(config);
        }
        if (config.node("version").getDouble() == 4.11d) {
            to412(config);
        }
        if (config.node("version").getDouble() == 4.12d) {
            to413(config);
        }
        if (config.node("version").getDouble() == 4.13d) {
            to50(config);
        }
        if (config.node("version").getDouble() == 5.0d) {
            to51(config);
        }
        if (config.node("version").getDouble() == 5.1d) {
            to52(config);
        }
        if (config.node("version").getDouble() == 5.2d) {
            to53(config);
        }

        if (config.node("version").getDouble() != oldVersion) {
            File backupFile = new File(fileOnDisk.getParent(), fileOnDisk.getName() + ".bak");
            if (backupFile.exists()) {
                java.nio.file.Files.delete(backupFile.toPath());
            }

            Files.copy(fileOnDisk, backupFile);
            loader.save(config);
        }
    }

    private static void to20(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Rabbit -> Messaging
        boolean rabbitEnabled = config.node("rabbit", "enabled").getBoolean();
        String rabbitAddress = config.node("rabbit", "address").getString("");
        int rabbitPort = config.node("rabbit", "port").getInt(5672);
        String rabbitUser = config.node("rabbit", "user").getString("guest");
        String rabbitPass = config.node("rabbit", "pass").getString("guest");
        config.removeChild("rabbit");
        config.node("messaging", "type").set((rabbitEnabled) ? "rabbit" : "bungee");
        config.node("messaging", "rabbit", "address").set(rabbitAddress);
        config.node("messaging", "rabbit", "port").set(rabbitPort);
        config.node("messaging", "rabbit", "user").set(rabbitUser);
        config.node("messaging", "rabbit", "pass").set(rabbitPass);

        // sources.order String -> List
        String[] order = config.node("sources", "order").getString("").split(",\\s?");
        config.node("sources", "order").setList(String.class, Arrays.asList(order));

        // Add ignore
        config.node("ignore").setList(String.class, Arrays.asList("127.0.0.1", "localhost", "::1"));

        // Remove async
        config.removeChild("async");

        // Version
        config.node("version").set(2.0d);
    }

    private static void to21(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Add consensus
        config.node("consensus").set(-1.0d);

        // Version
        config.node("version").set(2.1d);
    }

    private static void to22(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Add stats
        config.node("stats", "usage").set(Boolean.TRUE);
        config.node("stats", "errors").set(Boolean.TRUE);

        // Add update
        config.node("update", "check").set(Boolean.TRUE);
        config.node("update", "notify").set(Boolean.TRUE);

        // Version
        config.node("version").set(2.2d);
    }

    private static void to23(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Add voxprox
        config.node("sources", "voxprox", "enabled").set(Boolean.FALSE);
        config.node("sources", "voxprox", "key").set("");

        List<String> sources;
        try {
            sources = !config.node("sources", "order").empty() ? new ArrayList<>(config.node("sources", "order").getList(String.class)) : new ArrayList<>();
        } catch (SerializationException ex) {
            sources = new ArrayList<>();
        }
        if (!sources.contains("voxprox")) {
            sources.add("voxprox");
        }
        config.node("sources", "order").setList(String.class, sources);

        // Version
        config.node("version").set(2.3d);
    }

    private static void to33(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // sql -> storage
        String sqlType = config.node("sql", "type").getString("sqlite");
        int sqlThreads = config.node("sql", "threads").getInt(2);
        String sqlDatabase;
        if (sqlType.equalsIgnoreCase("sqlite")) {
            sqlDatabase = config.node("sql", "sqlite", "file").getString("avpn");
            int dotIndex = sqlDatabase.indexOf('.');
            if (dotIndex > 0) {
                sqlDatabase = sqlDatabase.substring(0, dotIndex);
            }
        } else {
            sqlDatabase = config.node("sql", "mysql", "database").getString("avpn");
        }
        String mysqlAddress = config.node("sql", "mysql", "address").getString("127.0.0.1");
        int mysqlPort = config.node("sql", "mysql", "port").getInt(3306);
        String mysqlUser = config.node("sql", "mysql", "user").getString("");
        String mysqlPass = config.node("sql", "mysql", "pass").getString("");
        config.removeChild("sql");
        config.node("storage", "method").set(sqlType);
        config.node("storage", "data", "address").set(mysqlAddress + ":" + mysqlPort);
        config.node("storage", "data", "database").set(sqlDatabase);
        config.node("storage", "data", "prefix").set("antivpn_");
        config.node("storage", "data", "username").set(mysqlUser);
        config.node("storage", "data", "password").set(mysqlPass);
        config.node("storage", "data", "mongodb", "collection-prefix").set("");
        config.node("storage", "data", "mongodb", "connection-uri").set("");
        config.node("storage", "settings", "max-pool-size").set(sqlThreads);
        config.node("storage", "settings", "min-idle").set(sqlThreads);
        config.node("storage", "settings", "max-lifetime").set(1800000L);
        config.node("storage", "settings", "timeout").set(5000L);
        config.node("storage", "settings", "properties", "unicode").set(Boolean.TRUE);
        config.node("storage", "settings", "properties", "encoding").set("utf8");

        // redis
        String redisAddress = config.node("redis", "address").getString("");
        if (redisAddress.isEmpty()) {
            redisAddress = "127.0.0.1";
        }
        int redisPort = config.node("redis", "port").getInt(6379);
        String redisPass = config.node("redis", "pass").getString("");
        config.node("redis").removeChild("port");
        config.node("redis").removeChild("pass");
        config.node("redis", "address").set(redisAddress + ":" + redisPort);
        config.node("redis", "password").set(redisPass);

        // messaging -> rabbitmq
        String messagingType = config.node("messaging", "type").getString("");
        String rabbitAddress = config.node("messaging", "rabbit", "address").getString("");
        if (rabbitAddress.isEmpty()) {
            rabbitAddress = "127.0.0.1";
        }
        int rabbitPort = config.node("messaging", "rabbit", "port").getInt(5672);
        String rabbitUser = config.node("messaging", "rabbit", "user").getString("guest");
        String rabbitPass = config.node("messaging", "rabbit", "pass").getString("guest");
        config.removeChild("messaging");
        config.node("rabbitmq", "enabled").set(messagingType.equalsIgnoreCase("rabbit") || messagingType.equalsIgnoreCase("rabbitmq") ? Boolean.TRUE : Boolean.FALSE);
        config.node("rabbitmq", "address").set(rabbitAddress + ":" + rabbitPort);
        config.node("rabbitmq", "username").set(rabbitUser);
        config.node("rabbitmq", "password").set(rabbitPass);

        // kick -> kick.enabled
        boolean kick = config.node("kick").getBoolean(true);
        config.removeChild("kick");
        config.node("kick", "enabled").set(kick);

        // ignore -> kick.ignore
        List<String> ignore;
        try {
            ignore = !config.node("ignore").empty() ? config.node("ignore").getList(String.class) : new ArrayList<>();
        } catch (SerializationException ex) {
            ignore = new ArrayList<>();
        }
        config.removeChild("ignore");
        config.node("kick", "ignore").setList(String.class, ignore);

        // kickMessage -> kick.message
        String kickMessage = config.node("kickMessage").getString("");
        config.removeChild("kickMessage");
        config.node("kick", "message").set(kickMessage);

        // consensus -> kick.algorithm
        double consensus = config.node("consensus").getDouble();
        config.node("kick", "algorithm", "method").set(consensus >= 0.0d ? "consensus" : "cascade");
        config.node("kick", "algorithm", "min-consensus").set(consensus >= 0.0d ? consensus : 0.6d);
        config.removeChild("consensus");

        // Add threads
        config.node("threads").set(4);

        // Version
        config.node("version").set(3.3d);
    }

    private static void to34(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Add storage->data->SSL
        config.node("storage", "data", "ssl").set(Boolean.FALSE);

        // Version
        config.node("version").set(3.4d);
    }

    private static void to35(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Remove IPDetector
        List<String> order = !config.node("sources", "order").empty() ? new ArrayList<>(config.node("sources", "order").getList(String.class)) : new ArrayList<>();

        List<String> removed = new ArrayList<>();
        for (String source : order) {
            if (source.equalsIgnoreCase("ipdetector")) { // sources are case-insensitive when loaded
                removed.add(source);
            }
        }

        order.removeAll(removed);
        config.node("sources", "order").setList(String.class, order);

        config.node("sources").removeChild("ipdetector");

        // Version
        config.node("version").set(3.5d);
    }

    private static void to36(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Add ipwarner
        config.node("sources", "ipwarner", "enabled").set(Boolean.FALSE);
        config.node("sources", "ipwarner", "key").set("");

        List<String> sources;
        try {
            sources = !config.node("sources", "order").empty() ? new ArrayList<>(config.node("sources", "order").getList(String.class)) : new ArrayList<>();
        } catch (SerializationException ex) {
            sources = new ArrayList<>();
        }
        if (!sources.contains("ipwarner")) {
            sources.add("ipwarner");
        }
        config.node("sources", "order").setList(String.class, sources);

        // Version
        config.node("version").set(3.6d);
    }

    private static void to37(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Remove kick->enabled
        boolean kickEnabled = config.node("kick", "enabled").getBoolean(true);

        // Rename kick->message to action->kick-message
        String kickMessage = config.node("kick", "message").getString("");
        config.node("action", "kick-message").set(kickEnabled ? kickMessage : "");

        // Rename kick->algorithm to action->algorithm
        String algorithmMethod = config.node("kick", "algorithm", "method").getString("");
        double algorithmConsensus = config.node("kick", "algorithm", "min-consensus").getDouble(0.6d);

        config.node("action", "algorithm", "method").set(algorithmMethod);
        config.node("action", "algorithm", "min-consensus").set(algorithmConsensus);

        // Rename kick->ignore to action->ignore
        List<String> ignore;
        try {
            ignore = !config.node("kick", "ignore").empty() ? new ArrayList<>(config.node("kick", "ignore").getList(String.class)) : new ArrayList<>();
        } catch (SerializationException ex) {
            ignore = new ArrayList<>();
        }

        config.node("action", "ignore").setList(String.class, ignore);

        // Remove kick
        config.removeChild("kick");

        // Add action->command
        config.node("action", "command").set("");

        // Version
        config.node("version").set(3.7d);
    }

    private static void to38(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Remove voxprox
        List<String> order = !config.node("sources", "order").empty() ? new ArrayList<>(config.node("sources", "order").getList(String.class)) : new ArrayList<>();

        List<String> removed = new ArrayList<>();
        for (String source : order) {
            if (source.equalsIgnoreCase("voxprox")) { // sources are case-insensitive when loaded
                removed.add(source);
            }
        }

        order.removeAll(removed);
        config.node("sources", "order").setList(String.class, order);

        config.node("sources").removeChild("voxprox");

        // Add teoh
        config.node("sources", "teoh", "enabled").set(Boolean.TRUE);

        // Add iphunter
        config.node("sources", "iphunter", "enabled").set(Boolean.FALSE);
        config.node("sources", "iphunter", "key").set("");
        config.node("sources", "iphunter", "block").set(1);

        List<String> sources;
        try {
            sources = !config.node("sources", "order").empty() ? new ArrayList<>(config.node("sources", "order").getList(String.class)) : new ArrayList<>();
        } catch (SerializationException ex) {
            sources = new ArrayList<>();
        }
        if (!sources.contains("teoh")) {
            sources.add("teoh");
        }
        if (!sources.contains("iphunter")) {
            sources.add("iphunter");
        }
        config.node("sources", "order").setList(String.class, sources);

        // Version
        config.node("version").set(3.8d);
    }

    private static void to39(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Add ip2proxy
        config.node("sources", "ip2proxy", "enabled").set(Boolean.TRUE);
        config.node("sources", "ip2proxy", "key").set("demo");

        List<String> sources;
        try {
            sources = !config.node("sources", "order").empty() ? new ArrayList<>(config.node("sources", "order").getList(String.class)) : new ArrayList<>();
        } catch (SerializationException ex) {
            sources = new ArrayList<>();
        }
        if (!sources.contains("ip2proxy")) {
            sources.add("ip2proxy");
        }
        config.node("sources", "order").setList(String.class, sources);

        // Version
        config.node("version").set(3.9d);
    }

    private static void to49(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Move storage
        String storageMethod = config.node("storage", "method").getString("sqlite");
        String storageAddress = config.node("storage", "data", "address").getString("127.0.0.1:3306");
        String storageDatabase = config.node("storage", "data", "database").getString("anti_vpn");
        String storagePrefix = config.node("storage", "data", "prefix").getString("avpn_");
        String storageUser = config.node("storage", "data", "username").getString("");
        String storagePass = config.node("storage", "data", "password").getString("");
        boolean storageSSL = config.node("storage", "data", "ssl").getBoolean(false);
        int storageMaxPoolSize = config.node("storage", "settings", "max-pool-size").getInt(4);
        int storageMinIdle = config.node("storage", "settings", "min-idle").getInt(4);
        long storageMaxLifetime = config.node("storage", "settings", "max-lifetime").getLong(1800000L);
        long storageTimeout = config.node("storage", "settings", "timeout").getLong(5000L);
        boolean storageUnicode = config.node("storage", "settings", "unicode").getBoolean(true);
        String storageEncoding = config.node("storage", "settings", "encoding").getString("utf8");

        config.removeChild("storage");

        config.node("storage", "engines", "mysql", "enabled").set(storageMethod.equalsIgnoreCase("mysql"));
        config.node("storage", "engines", "mysql", "connection", "address").set(storageAddress);
        config.node("storage", "engines", "mysql", "connection", "database").set(storageDatabase);
        config.node("storage", "engines", "mysql", "connection", "prefix").set(storagePrefix);
        config.node("storage", "engines", "mysql", "connection", "username").set(storageUser);
        config.node("storage", "engines", "mysql", "connection", "password").set(storagePass);
        config.node("storage", "engines", "mysql", "connection", "options")
                .set("useSSL=" + storageSSL + "&useUnicode=" + storageUnicode + "&characterEncoding=" + storageEncoding);
        config.node("storage", "engines", "redis", "enabled").set(Boolean.FALSE);
        config.node("storage", "engines", "redis", "connection", "address").set("127.0.0.1:6379");
        config.node("storage", "engines", "redis", "connection", "password").set("");
        config.node("storage", "engines", "redis", "connection", "prefix").set("avpn:");
        config.node("storage", "engines", "sqlite", "enabled").set(storageMethod.equalsIgnoreCase("sqlite"));
        config.node("storage", "engines", "sqlite", "connection", "file").set(storageDatabase + ".db");
        config.node("storage", "engines", "sqlite", "connection", "prefix").set(storagePrefix);
        config.node("storage", "engines", "sqlite", "connection", "options").set("useUnicode=" + storageUnicode + "&characterEncoding=" + storageEncoding);
        config.node("storage", "settings", "max-pool-size").set(storageMaxPoolSize);
        config.node("storage", "settings", "min-idle").set(storageMinIdle);
        config.node("storage", "settings", "max-lifetime").set(storageMaxLifetime);
        config.node("storage", "settings", "timeout").set(storageTimeout);
        config.node("storage", "order").set(Arrays.asList("mysql", "redis", "sqlite"));

        // Move messaging
        boolean redisEnabled = config.node("redis", "enabled").getBoolean(false);
        String redisAddress = config.node("redis", "address").getString("127.0.0.1:6379");
        String redisPass = config.node("redis", "password").getString("");
        boolean rabbitEnabled = config.node("rabbitmq", "enabled").getBoolean(false);
        String rabbitAddress = config.node("rabbitmq", "address").getString("127.0.0.1:5672");
        String rabbitUser = config.node("rabbitmq", "username").getString("guest");
        String rabbitPass = config.node("rabbitmq", "password").getString("guest");
        int messagingMaxPoolSize = config.node("messaging", "settings", "max-pool-size").getInt(4);
        int messagingMinIdle = config.node("messaging", "settings", "min-idle").getInt(4);
        long messagingMaxLifetime = config.node("messaging", "settings", "max-lifetime").getLong(1800000L);
        long messagingTimeout = config.node("messaging", "settings", "timeout").getLong(5000L);

        config.removeChild("redis");
        config.removeChild("rabbitmq");

        config.node("messaging", "engines", "redis", "enabled").set(redisEnabled);
        config.node("messaging", "engines", "redis", "connection", "address").set(redisAddress);
        config.node("messaging", "engines", "redis", "connection", "password").set(redisPass);
        config.node("messaging", "engines", "rabbitmq", "enabled").set(rabbitEnabled);
        config.node("messaging", "engines", "rabbitmq", "connection", "address").set(rabbitAddress);
        config.node("messaging", "engines", "rabbitmq", "connection", "v-host").set("/");
        config.node("messaging", "engines", "rabbitmq", "connection", "username").set(rabbitUser);
        config.node("messaging", "engines", "rabbitmq", "connection", "password").set(rabbitPass);
        config.node("messaging", "settings", "max-pool-size").set(messagingMaxPoolSize);
        config.node("messaging", "settings", "min-idle").set(messagingMinIdle);
        config.node("messaging", "settings", "max-lifetime").set(messagingMaxLifetime);
        config.node("messaging", "settings", "timeout").set(messagingTimeout);
        config.node("messaging", "order").set(Arrays.asList("rabbitmq", "redis"));

        // action to action->vpn, action->command to action->vpn->commands
        String kickMessage = config.node("action", "kick-message").getString("&cPlease disconnect from your proxy or VPN before re-joining!");
        String algorithmMethod = config.node("action", "algorithm", "method").getString("cascade");
        double algorithmMinConsensus = config.node("action", "algorithm", "min-consensus").getDouble(0.6d);
        String actionCommand = config.node("action", "command").getString("");

        config.node("action").removeChild("kick-message");
        config.node("action").removeChild("command");
        config.node("action").removeChild("algorithm");

        config.node("action", "vpn", "kick-message").set(kickMessage);
        config.node("action", "vpn", "commands").set(Collections.singleton(actionCommand));
        config.node("action", "vpn", "algorithm", "method").set(algorithmMethod);
        config.node("action", "vpn", "algorithm", "min-consensus").set(algorithmMinConsensus);

        // sources->cacheTime to sources->cache-time
        String sourceCacheTime = config.node("sources", "cacheTime").getString("6hours");
        config.node("sources").removeChild("cacheTime");
        config.node("sources", "cache-time").set(sourceCacheTime);

        // cacheTime & threads to connection->cache-time & threads, add timeout
        String cacheTime = config.node("cacheTime").getString("1minute");
        int threads = config.node("threads").getInt(4);
        config.removeChild("cacheTime");
        config.removeChild("threads");
        config.node("connection", "cache-time").set(cacheTime);
        config.node("connection", "threads").set(threads);
        config.node("connection", "timeout").set(5000L);

        // sources->cacheTime to sources->cache-time
        String sourcesCacheTime = config.node("sources", "cacheTime").getString("6hours");
        config.node("sources").removeChild("cacheTime");
        config.node("sources", "cache-time").set(sourcesCacheTime);

        // Add mcleaks
        config.node("mcleaks", "cache-time").set("1day");
        config.node("mcleaks", "key").set("");
        config.node("action", "mcleaks", "kick-message").set("&cPlease discontinue your use of an MCLeaks account!");
        config.node("action", "mcleaks", "commands").set(Collections.singleton(""));

        // Remove localhost from ignore
        List<String> ignoredIPs = !config.node("action", "ignore").empty() ? new ArrayList<>(config.node("action", "ignore").getList(String.class)) : new ArrayList<>();

        List<String> removed = new ArrayList<>();
        for (String ip : ignoredIPs) {
            if (ip.equalsIgnoreCase("localhost")) { // IPs are case-insensitive when loaded
                removed.add(ip);
            }
        }

        ignoredIPs.removeAll(removed);
        config.node("action", "ignore").setList(String.class, ignoredIPs);

        // Version
        config.node("version").set(4.9d);
    }

    private static void to411(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Add lang
        config.node("lang").set("en");

        // Version
        config.node("version").set(4.11d);
    }

    private static void to412(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Add proxy to ipqualityscore
        config.node("sources", "ipqualityscore", "proxy").set(Boolean.FALSE);

        // Add mobile to ipqualityscore
        config.node("sources", "ipqualityscore", "mobile").set(Boolean.TRUE);

        // Add strictness to ipqualityscore
        config.node("sources", "ipqualityscore", "strictness").set(0);

        // Add recent-abuse to ipqualityscore
        config.node("sources", "ipqualityscore", "recent-abuse").set(Boolean.TRUE);

        // Forcibly change ipqualityscore from (default) 65% to 98% - this will override any previous customizations
        config.node("sources", "ipqualityscore", "threshold").set(0.98d);

        // Version
        config.node("version").set(4.12d);
    }

    private static void to413(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Add private ranges to ignore
        List<String> ignoredIPs = !config.node("action", "ignore").empty() ? new ArrayList<>(config.node("action", "ignore").getList(String.class)) : new ArrayList<>();

        List<String> added = new ArrayList<>();
        added.add("10.0.0.0/8");
        added.add("172.16.0.0/12");
        added.add("192.168.0.0/16");
        added.add("fd00::/8");

        for (Iterator<String> i = added.iterator(); i.hasNext(); ) {
            String ip = i.next();
            for (String ip2 : ignoredIPs) {
                if (ip.equalsIgnoreCase(ip2)) { // IPs are case-insensitive when loaded
                    i.remove();
                }
            }
        }

        ignoredIPs.addAll(added);
        config.node("action", "ignore").setList(String.class, ignoredIPs);

        // Version
        config.node("version").set(4.13d);
    }

    private static void to50(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // storage->engines->type to storage->engines->name
        config.node("storage", "engines", "engine1").from(config.node("storage", "engines", "mysql"));
        config.node("storage", "engines", "engine2").from(config.node("storage", "engines", "sqlite"));
        config.node("storage", "engines", "engine1", "type").set("mysql");
        config.node("storage", "engines", "engine2", "type").set("sqlite");
        config.node("storage", "engines").removeChild("mysql");
        config.node("storage", "engines").removeChild("redis");
        config.node("storage", "engines").removeChild("sqlite");

        // Modify storage->order for new values
        List<String> storageOrder = !config.node("storage", "order").empty() ? new ArrayList<>(config.node("storage", "order").getList(String.class)) : new ArrayList<>();
        List<String> newStorageOrder = new ArrayList<>();
        for (String engine : storageOrder) {
            switch (engine) {
                case "mysql":
                    newStorageOrder.add("engine1");
                    break;
                case "sqlite":
                    newStorageOrder.add("engine2");
                    break;
                default:
                    break;
            }
        }
        config.node("storage", "order").setList(String.class, newStorageOrder);

        // Make storage->settings->max-lifetime/timeout more readable
        config.node("storage", "settings", "max-lifetime")
                .set(TimeUtil.getTimeString(new TimeUtil.Time((config.node("storage", "settings", "max-lifetime").getLong(1800000L) / 1000L) / 60L, TimeUnit.MINUTES)));
        config.node("storage", "settings", "timeout")
                .set(TimeUtil.getTimeString(new TimeUtil.Time(config.node("storage", "settings", "timeout").getLong(5000L) / 1000L, TimeUnit.SECONDS)));

        // Remove storage->engines->name->connection->prefix
        config.node("storage", "engines", "engine1", "connection").removeChild("prefix");
        config.node("storage", "engines", "engine2", "connection").removeChild("prefix");

        // messaging->engines->type to messaging->engines->name
        config.node("messaging", "engines", "engine1").from(config.node("messaging", "engines", "rabbitmq"));
        config.node("messaging", "engines", "engine2").from(config.node("messaging", "engines", "redis"));
        config.node("messaging", "engines", "engine1", "type").set("rabbitmq");
        config.node("messaging", "engines", "engine2", "type").set("redis");
        config.node("messaging", "engines").removeChild("rabbitmq");
        config.node("messaging", "engines").removeChild("redis");

        // Remove messaging->order
        config.node("messaging").removeChild("order");

        // Make messaging->settings->max-lifetime/timeout more readable
        config.node("messaging", "settings", "max-lifetime")
                .set(TimeUtil.getTimeString(new TimeUtil.Time((config.node("messaging", "settings", "max-lifetime").getLong(1800000L) / 1000L) / 60L, TimeUnit.MINUTES)));
        config.node("messaging", "settings", "timeout")
                .set(TimeUtil.getTimeString(new TimeUtil.Time(config.node("messaging", "settings", "timeout").getLong(5000L) / 1000L, TimeUnit.SECONDS)));

        // Make connection->timeout more readable
        config.node("connection", "timeout")
                .set(TimeUtil.getTimeString(new TimeUtil.Time(config.node("connection", "timeout").getLong(5000L) / 1000L, TimeUnit.SECONDS)));

        // Add ipinfo
        config.node("sources", "ipinfo", "enabled").set(Boolean.FALSE);
        config.node("sources", "ipinfo", "key").set("");
        config.node("sources", "ipinfo", "proxy").set(Boolean.TRUE);

        List<String> sources = !config.node("sources", "order").empty() ? new ArrayList<>(config.node("sources", "order").getList(String.class)) : new ArrayList<>();
        if (!sources.contains("ipinfo")) {
            sources.add("ipinfo");
        }
        config.node("sources", "order").setList(String.class, sources);

        // Remove ipwarner
        List<String> order = !config.node("sources", "order").empty() ? new ArrayList<>(config.node("sources", "order").getList(String.class)) : new ArrayList<>();

        List<String> removed = new ArrayList<>();
        for (String source : order) {
            if (source.equalsIgnoreCase("ipwarner")) { // sources are case-insensitive when loaded
                removed.add(source);
            }
        }

        order.removeAll(removed);
        config.node("sources", "order").setList(String.class, order);

        config.node("sources").removeChild("ipwarner");

        // Add "subdomain" to sources->getipintel
        config.node("sources", "getipintel", "subdomain").set("check");

        // Version
        config.node("version").set(5.0d);
    }

    private static void to51(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Set default engine2 from SQLite to H2
        String engine2 = config.node("storage", "engines", "engine2", "type").getString();
        if ("sqlite".equalsIgnoreCase(engine2)) {
            config.node("storage", "engines", "engine2", "type").set("h2");
            config.node("storage", "engines", "engine2", "connection", "file").set("anti_vpn");
        }

        // Version
        config.node("version").set(5.1d);
    }

    private static void to52(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Update lang from en to en-US
        if (config.node("lang").getString("en").equalsIgnoreCase("en")) {
            config.node("lang").set("en-US");
        }

        // Update action->vpn/mcleaks->kick-message to use minimessage format
        if (config.node("action", "vpn", "kick-message")
                .getString("&cPlease disconnect from your proxy or VPN before re-joining!")
                .equalsIgnoreCase("&cPlease disconnect from your proxy or VPN before re-joining!")) {
            config.node("action", "vpn", "kick-message").set("<red>Please disconnect from your proxy or VPN before re-joining!</red>");
        }
        if (config.node("action", "mcleaks", "kick-message")
                .getString("&cPlease discontinue your use of an MCLeaks account!")
                .equalsIgnoreCase("&cPlease discontinue your use of an MCLeaks account!")) {
            config.node("action", "mcleaks", "kick-message").set("<red>Please discontinue your use of an MCLeaks account!</red>");
        }

        // Add permissions->admin/bypass
        config.node("permissions", "admin").set("avpn.admin");
        config.node("permissions", "bypass").set("avpn.bypass");

        // Add aliases
        config.node("aliases", "base").setList(String.class, Collections.singletonList("avpn"));
        config.node("aliases", "help").setList(String.class, Collections.singletonList(""));
        config.node("aliases", "reload").setList(String.class, Collections.singletonList(""));
        config.node("aliases", "import").setList(String.class, Collections.singletonList(""));
        config.node("aliases", "kick").setList(String.class, Collections.singletonList(""));
        config.node("aliases", "test").setList(String.class, Collections.singletonList(""));
        config.node("aliases", "score").setList(String.class, Collections.singletonList(""));
        config.node("aliases", "check").setList(String.class, Collections.singletonList(""));

        // Version
        config.node("version").set(5.2d);
    }

    private static void to53(@NotNull CommentedConfigurationNode config) throws SerializationException {
        // Add messaging->settings->delay
        config.node("messaging", "settings", "delay").set("1second");
        // Add messaging->settings->redundancy
        config.node("messaging", "settings", "redundancy").set(Boolean.TRUE);

        // Version
        config.node("version").set(5.3d);
    }
}

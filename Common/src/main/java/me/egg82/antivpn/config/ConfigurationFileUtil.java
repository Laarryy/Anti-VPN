package me.egg82.antivpn.config;

import co.aikar.commands.CommandIssuer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.messaging.*;
import me.egg82.antivpn.storage.MySQLStorageService;
import me.egg82.antivpn.storage.SQLiteStorageService;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.PacketUtil;
import me.egg82.antivpn.utils.TimeUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.reflect.PackageFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import redis.clients.jedis.exceptions.JedisException;

public class ConfigurationFileUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationFileUtil.class);

    private ConfigurationFileUtil() { }

    public static void reloadConfig(@NonNull File dataDirectory, @NonNull CommandIssuer console, @NonNull MessagingHandler messagingHandler, @NonNull SourceManager sourceManager) {
        ConfigurationNode config;
        try {
            config = getConfig("config.yml", new File(dataDirectory, "config.yml"));
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        boolean debug = config.node("debug").getBoolean(false);
        if (!debug) {
            Reflections.log = null;
        }
        if (debug) {
            console.sendMessage(LogUtil.HEADING + "<c2>Debug</c2> <c1>enabled</c1>");
        }

        UUID serverId = ServerIDUtil.getId(new File(dataDirectory, "server-id.txt"));
        if (debug) {
            console.sendMessage(LogUtil.HEADING + "<c2>Server ID:</c2> <c1>" + serverId.toString() + "</c1>");
        }

        AlgorithmMethod vpnAlgorithmMethod = getVpnAlgorithmMethod(config, debug, console);

        CachedConfig cachedConfig = CachedConfig.builder()
                .debug(debug)
                .language(getLanguage(config, debug, console))
                .storage(getStorage(config, dataDirectory, debug, console))
                .messaging(getMessaging(config, serverId, messagingHandler, debug, console))
                .sourceCacheTime(getSourceCacheTime(config, debug, console))
                .mcleaksCacheTime(getMcLeaksCacheTime(config, debug, console))
                .ignoredIps(getIgnoredIps(config, debug, console))
                .cacheTime(getCacheTime(config, debug, console))
                .threads(config.node("connection", "threads").getInt(4))
                .timeout(config.node("connection", "timeout").getLong(5000L))
                .vpnKickMessage(config.node("action", "vpn", "kick-message").getString("&cPlease disconnect from your proxy or VPN before re-joining!"))
                .vpnActionCommands(getVpnActionCommands(config, debug, console))
                .mcleaksKickMessage(config.node("action", "mcleaks", "kick-message").getString("&cPlease discontinue your use of an MCLeaks account!"))
                .mcleaksActionCommands(getMcLeaksActionCommands(config, debug, console))
                .vpnAlgorithmMethod(vpnAlgorithmMethod)
                .vpnAlgorithmConsensus(getVpnAlgorithmConsensus(config, vpnAlgorithmMethod == AlgorithmMethod.CONSESNSUS, debug, console))
                .mcleaksKey(config.node("mcleaks", "key").getString(""))
                .serverId(serverId)
                .build();

        PacketUtil.setPoolSize(cachedConfig.getMessaging().size() + 1);

        ConfigUtil.setConfiguration(config, cachedConfig);

        setSources(config, debug, console, sourceManager);

        if (debug) {
            console.sendMessage(LogUtil.HEADING + "<c2>Source threads:</c2> <c1>" + cachedConfig.getThreads() + "</c1>");
            console.sendMessage(LogUtil.HEADING + "<c2>Source timeout:</c2> <c1>" + cachedConfig.getTimeout() + "ms</c1>");
        }
    }

    private static @NonNull Locale getLanguage(@NonNull ConfigurationNode config, boolean debug, @NonNull CommandIssuer console) {
        String configLanguage = config.node("lang").getString("en");
        Locale retVal = null;
        for (Locale locale : Locale.getAvailableLocales()) {
            String l = locale.getCountry() == null || locale.getCountry().isEmpty() ? locale.getLanguage() : locale.getLanguage() + "-" + locale.getCountry();
            if (locale.getLanguage().equalsIgnoreCase(configLanguage) || l.equalsIgnoreCase(configLanguage)) {
                retVal = locale;
                break;
            }
        }

        if (retVal == null) {
            retVal = Locale.ENGLISH;
            console.sendMessage(LogUtil.HEADING + "<c9>lang</c9> <c1>" + configLanguage + "</c1> <c9>is not a valid language. Using default value of</c9> <c1>" + (retVal.getCountry() == null || retVal.getCountry().isEmpty() ? retVal.getLanguage() : retVal.getLanguage() + "-" + retVal.getCountry()) + "</c1>");
        }
        if (debug) {
            console.sendMessage(LogUtil.HEADING + "<c2>Default language:</c2> <c1>" + (retVal.getCountry() == null || retVal.getCountry().isEmpty() ? retVal.getLanguage() : retVal.getLanguage() + "-" + retVal.getCountry()) + "</c1>");
        }

        return retVal;
    }

    private static @NonNull List<StorageService> getStorage(@NonNull ConfigurationNode config, @NonNull File dataDirectory, boolean debug, @NonNull CommandIssuer console) {
        List<StorageService> retVal = new ArrayList<>();

        PoolSettings poolSettings = new PoolSettings(config.node("storage", "settings"));
        for (Map.Entry<Object, ? extends ConfigurationNode> kvp : config.node("storage", "engines").childrenMap().entrySet()) {
            StorageService service = getStorageOf((String) kvp.getKey(), kvp.getValue(), dataDirectory, poolSettings, debug, console);
            if (service == null) {
                continue;
            }

            if (debug) {
                console.sendMessage(LogUtil.HEADING + "<c2>Added storage:</c2> <c1>" + service.getName() + " (" + service.getClass().getSimpleName() + ")</c1>");
            }
            retVal.add(service);
        }

        return retVal;
    }

    private static @Nullable StorageService getStorageOf(@NonNull String name, @NonNull ConfigurationNode engineNode, @NonNull File dataDirectory, @NonNull PoolSettings poolSettings, boolean debug, @NonNull CommandIssuer console) {
        if (!engineNode.node("enabled").getBoolean()) {
            if (debug) {
                console.sendMessage(LogUtil.HEADING + "<c9>Engine</c9> <c1>" + name + "</c1> <c9>is disabled. Removing.</c9>");
            }
            return null;
        }

        String type = engineNode.node("type").getString("").toLowerCase();
        ConfigurationNode connectionNode = engineNode.node("connection");
        switch (type) {
            case "mysql": {
                AddressPort url = new AddressPort(connectionNode.key() + ".address", connectionNode.node("address").getString("127.0.0.1:3306"), 3306);
                if (debug) {
                    console.sendMessage(LogUtil.HEADING + "<c2>Creating engine</c2> <c1>" + name + "</c1> <c2>of type mysql with address</c2> <c1>" + url.getAddress() + ":" + url.getPort() + "/" + connectionNode.node("database").getString("anti_vpn") + "</c1>");
                }
                String options = connectionNode.node("options").getString("useSSL=false&useUnicode=true&characterEncoding=utf8");
                if (options.length() > 0 && options.charAt(0) == '?') {
                    options = options.substring(1);
                }
                if (debug) {
                    console.sendMessage(LogUtil.HEADING + "<c2>Setting options for engine</c2> <c1>" + name + "</c1> <c2>to</c2> <c1>" + options.replace("&", "&&") + "</c1>");
                }
                try {
                    return MySQLStorageService.builder(name)
                            .url(url.address, url.port, connectionNode.node("database").getString("anti_vpn"))
                            .credentials(connectionNode.node("username").getString(""), connectionNode.node("password").getString(""))
                            .options(options)
                            .poolSize(poolSettings.minPoolSize, poolSettings.maxPoolSize)
                            .life(poolSettings.maxLifetime, poolSettings.timeout)
                            .build();
                } catch (IOException ex) {
                    logger.error("Could not create engine \"" + name + "\".", ex);
                }
                break;
            }
            case "sqlite": {
                AddressPort url = new AddressPort(connectionNode.key() + ".address", connectionNode.node("address").getString("127.0.0.1:6379"), 6379);
                if (debug) {
                    console.sendMessage(LogUtil.HEADING + "<c2>Creating engine</c2> <c1>" + name + "</c1> <c2>of type redis with address</c2> <c1>" + url.getAddress() + ":" + url.getPort() + "</c1>");
                }
                String options = connectionNode.node("options").getString("useUnicode=true&characterEncoding=utf8");
                if (options.length() > 0 && options.charAt(0) == '?') {
                    options = options.substring(1);
                }
                if (debug) {
                    console.sendMessage(LogUtil.HEADING + "<c2>Setting options for engine</c2> <c1>" + name + "</c1> <c2>to</c2> <c1>" + options.replace("&", "&&") + "</c1>");
                }
                try {
                    return SQLiteStorageService.builder(name)
                            .file(new File(dataDirectory, connectionNode.node("file").getString("anti_vpn.db")))
                            .options(options)
                            .poolSize(poolSettings.minPoolSize, poolSettings.maxPoolSize)
                            .life(poolSettings.maxLifetime, poolSettings.timeout)
                            .build();
                } catch (IOException ex) {
                    logger.error("Could not create engine \"" + name + "\".", ex);
                }
                break;
            }
            default: {
                console.sendMessage(LogUtil.HEADING + "<c9>Unknown storage type</c9> <c1>" + type + "</c1> <c9>in engine</c9> <c1>" + name + "</c1>");
                break;
            }
        }
        return null;
    }

    private static @NonNull List<MessagingService> getMessaging(@NonNull ConfigurationNode config, @NonNull UUID serverId, @NonNull MessagingHandler handler, boolean debug, @NonNull CommandIssuer console) {
        List<MessagingService> retVal = new ArrayList<>();

        PoolSettings poolSettings = new PoolSettings(config.node("messaging", "settings"));
        for (Map.Entry<Object, ? extends ConfigurationNode> kvp : config.node("messaging", "engines").childrenMap().entrySet()) {
            MessagingService service = getMessagingOf((String) kvp.getKey(), kvp.getValue(), serverId, handler, poolSettings, debug, console);
            if (service == null) {
                continue;
            }

            if (debug) {
                console.sendMessage(LogUtil.HEADING + "<c2>Added messaging:</c2> <c1>" + service.getName() + " (" + service.getClass().getSimpleName() + ")</c1>");
            }
            retVal.add(service);
        }

        return retVal;
    }

    private static @Nullable MessagingService getMessagingOf(@NonNull String name, @NonNull ConfigurationNode engineNode, @NonNull UUID serverId, @NonNull MessagingHandler handler, @NonNull PoolSettings poolSettings, boolean debug, @NonNull CommandIssuer console) {
        if (!engineNode.node("enabled").getBoolean()) {
            if (debug) {
                console.sendMessage(LogUtil.HEADING + "<c9>Engine</c9> <c1>" + name + "</c1> <c9>is disabled. Removing.</c9>");
            }
            return null;
        }

        String type = engineNode.node("type").getString("").toLowerCase();
        ConfigurationNode connectionNode = engineNode.node("connection");
        switch (type) {
            case "rabbitmq": {
                AddressPort url = new AddressPort(connectionNode.key() + ".address", connectionNode.node("address").getString("127.0.0.1:5672"), 5672);
                if (debug) {
                    console.sendMessage(LogUtil.HEADING + "<c2>Creating engine</c2> <c1>" + name + "</c1> <c2>of type rabbitmq with address</c2> <c1>" + url.getAddress() + ":" + url.getPort() + connectionNode.node("v-host").getString("/") + "</c1>");
                }
                try {
                    return RabbitMQMessagingService.builder(name, serverId, handler)
                            .url(url.address, url.port, connectionNode.node("v-host").getString("/"))
                            .credentials(connectionNode.node("username").getString("guest"), connectionNode.node("password").getString("guest"))
                            .timeout((int) poolSettings.timeout)
                            .build();
                } catch (IOException | TimeoutException ex) {
                    logger.error("Could not create engine \"" + name + "\".", ex);
                }
                break;
            }
            case "redis": {
                AddressPort url = new AddressPort(connectionNode.key() + ".address", connectionNode.node("address").getString("127.0.0.1:6379"), 6379);
                if (debug) {
                    console.sendMessage(LogUtil.HEADING + "<c2>Creating engine</c2> <c1>" + name + "</c1> <c2>of type redis with address</c2> <c1>" + url.getAddress() + ":" + url.getPort() + "</c1>");
                }
                try {
                    return RedisMessagingService.builder(name, serverId, handler)
                            .url(url.address, url.port)
                            .credentials(connectionNode.node("password").getString(""))
                            .poolSize(poolSettings.minPoolSize, poolSettings.maxPoolSize)
                            .life(poolSettings.maxLifetime, (int) poolSettings.timeout)
                            .build();
                } catch (JedisException ex) {
                    logger.error("Could not create engine \"" + name + "\".", ex);
                }
                break;
            }
            default: {
                console.sendMessage(LogUtil.HEADING + "<c9>Unknown messaging type</c9> <c1>" + type + "</c1> <c9>in engine</c9> <c1>" + name + "</c1>");
                break;
            }
        }
        return null;
    }

    private static TimeUtil.Time getSourceCacheTime(@NonNull ConfigurationNode config, boolean debug, @NonNull CommandIssuer console) {
        TimeUtil.Time retVal = TimeUtil.getTime(config.node("sources", "cache-time").getString("6hours"));
        if (retVal == null) {
            console.sendMessage(LogUtil.HEADING + "<c2>sources.cache-time is not a valid time pattern. Using default value.<c2>");
            retVal = new TimeUtil.Time(6L, TimeUnit.HOURS);
        }

        if (debug) {
            console.sendMessage(LogUtil.HEADING + "<c2>Source cache time:</c2> <c1>" + retVal.getMillis() + "ms (" + retVal.getTime() + " " + retVal.getUnit().name() + ")</c1>");
        }
        return retVal;
    }

    private static TimeUtil.Time getMcLeaksCacheTime(@NonNull ConfigurationNode config, boolean debug, @NonNull CommandIssuer console) {
        TimeUtil.Time retVal = TimeUtil.getTime(config.node("mcleaks", "cache-time").getString("1day"));
        if (retVal == null) {
            console.sendMessage(LogUtil.HEADING + "<c2>mcleaks.cache-time is not a valid time pattern. Using default value.<c2>");
            retVal = new TimeUtil.Time(1L, TimeUnit.DAYS);
        }

        if (debug) {
            console.sendMessage(LogUtil.HEADING + "<c2>MCLeaks cache time:</c2> <c1>" + retVal.getMillis() + "ms (" + retVal.getTime() + " " + retVal.getUnit().name() + ")</c1>");
        }
        return retVal;
    }

    private static TimeUtil.Time getCacheTime(@NonNull ConfigurationNode config, boolean debug, @NonNull CommandIssuer console) {
        TimeUtil.Time retVal = TimeUtil.getTime(config.node("connection", "cache-time").getString("1minute"));
        if (retVal == null) {
            console.sendMessage(LogUtil.HEADING + "<c2>connection.cache-time is not a valid time pattern. Using default value.<c2>");
            retVal = new TimeUtil.Time(1L, TimeUnit.MINUTES);
        }

        if (debug) {
            console.sendMessage(LogUtil.HEADING + "<c2>Memory cache time:</c2> <c1>" + retVal.getMillis() + "ms (" + retVal.getTime() + " " + retVal.getUnit().name() + ")</c1>");
        }
        return retVal;
    }

    private static @NonNull Set<String> getIgnoredIps(@NonNull ConfigurationNode config, boolean debug, @NonNull CommandIssuer console) {
        Set<String> retVal;
        try {
            retVal = new HashSet<>(!config.node("ignore", "ips").empty() ? config.node("ignore", "ips").getList(String.class) : new ArrayList<>());
        } catch (SerializationException ex) {
            logger.error(ex.getMessage(), ex);
            retVal = new HashSet<>();
        }

        for (Iterator<String> i = retVal.iterator(); i.hasNext();) {
            String ip = i.next();
            if (!ValidationUtil.isValidIp(ip) && !ValidationUtil.isValidIpRange(ip)) {
                if (debug) {
                    console.sendMessage(LogUtil.HEADING + "<c9>Removed invalid ignore IP/range:</c9> <c1>" + ip + "</c1>");
                }
                i.remove();
            } else {
                if (debug) {
                    console.sendMessage(LogUtil.HEADING + "<c2>Adding ignored IP or range:</c2> <c1>" + ip + "</c1>");
                }
            }
        }

        return retVal;
    }

    private static @NonNull Set<String> getVpnActionCommands(@NonNull ConfigurationNode config, boolean debug, @NonNull CommandIssuer console) {
        Set<String> retVal;
        try {
            retVal = new HashSet<>(!config.node("action", "vpn", "commands").empty() ? config.node("action", "vpn", "commands").getList(String.class) : new ArrayList<>());
        } catch (SerializationException ex) {
            logger.error(ex.getMessage(), ex);
            retVal = new HashSet<>();
        }
        retVal.removeIf(action -> action == null || action.isEmpty());

        if (debug) {
            for (String action : retVal) {
                console.sendMessage(LogUtil.HEADING + "<c2>Adding command action for VPN usage:</c2> <c1>" + action + "</c1>");
            }
        }

        return retVal;
    }

    private static @NonNull Set<String> getMcLeaksActionCommands(@NonNull ConfigurationNode config, boolean debug, @NonNull CommandIssuer console) {
        Set<String> retVal;
        try {
            retVal = new HashSet<>(!config.node("action", "mcleaks", "commands").empty() ? config.node("action", "mcleaks", "commands").getList(String.class) : new ArrayList<>());
        } catch (SerializationException ex) {
            logger.error(ex.getMessage(), ex);
            retVal = new HashSet<>();
        }
        retVal.removeIf(action -> action == null || action.isEmpty());

        if (debug) {
            for (String action : retVal) {
                console.sendMessage(LogUtil.HEADING + "<c2>Adding command action for MCLeaks usage:</c2> <c1>" + action + "</c1>");
            }
        }

        return retVal;
    }

    private static @NonNull AlgorithmMethod getVpnAlgorithmMethod(@NonNull ConfigurationNode config, boolean debug, @NonNull CommandIssuer console) {
        AlgorithmMethod retVal = AlgorithmMethod.getByName(config.node("action", "vpn", "algorithm", "method").getString("cascade"));
        if (retVal == null) {
            console.sendMessage(LogUtil.HEADING + "<c2>action.vpn.algorithm.method is not a valid type. Using default value.<c2>");
            retVal = AlgorithmMethod.CASCADE;
        }

        if (debug) {
            console.sendMessage(LogUtil.HEADING + "<c2>Using VPN algorithm:</c2> <c1>" + retVal.name() + "</c1>");
        }

        return retVal;
    }

    private static double getVpnAlgorithmConsensus(ConfigurationNode config, boolean consensus, boolean debug, CommandIssuer console) {
        double retVal = config.node("action", "vpn", "algorithm", "min-consensus").getDouble(0.6d);
        retVal = Math.max(0.0d, Math.min(1.0d, retVal));

        if (consensus && debug) {
            console.sendMessage(LogUtil.HEADING + "<c2>Using consensus value:</c2> <c1>" + retVal + "</c1>");
        }

        return retVal;
    }

    private static void setSources(@NonNull ConfigurationNode config, boolean debug, @NonNull CommandIssuer console, @NonNull SourceManager sourceManager) {
        Map<String, Source<? extends SourceModel>> initializedSources = new HashMap<>();

        List<Class<Source>> sourceClasses = PackageFilter.getClasses(Source.class, "me.egg82.antivpn.api.model.source.models", false, false, false);
        for (Class<Source> clazz : sourceClasses) {
            if (debug) {
                console.sendMessage(LogUtil.HEADING + "<c2>Initializing source</c2> <c1>" + clazz.getSimpleName() + "</c1>");
            }

            try {
                Source<? extends SourceModel> source = (Source<? extends SourceModel>) clazz.newInstance();
                initializedSources.put(source.getName(), source);
            } catch (InstantiationException | IllegalAccessException | ClassCastException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        List<String> order;
        try {
            order = !config.node("sources", "order").empty() ? new ArrayList<>(config.node("sources", "order").getList(String.class)) : new ArrayList<>();
        } catch (SerializationException ex) {
            logger.error(ex.getMessage(), ex);
            order = new ArrayList<>();
        }

        for (Iterator<String> i = order.iterator(); i.hasNext();) {
            String s = i.next();
            if (!config.node("sources", s, "enabled").getBoolean(false)) {
                if (debug) {
                    console.sendMessage(LogUtil.HEADING + "<c9>Source " + s + " is disabled. Removing.</c9>");
                }
                sourceManager.deregisterSource(s);
                i.remove();
                continue;
            }

            Source<? extends SourceModel> source = initializedSources.get(s);
            if (source == null) {
                if (debug) {
                    console.sendMessage(LogUtil.HEADING + "<c9>Source " + s + " was not found. Removing.</c9>");
                }
                sourceManager.deregisterSource(s);
                i.remove();
                continue;
            }

            if (source.isKeyRequired() && config.node("sources", s, "key").getString("").isEmpty()) {
                if (debug) {
                    console.sendMessage(LogUtil.HEADING + "<c9>Source " + s + " requires a key which was not provided. Removing.</c9>");
                }
                sourceManager.deregisterSource(s);
                i.remove();
            }
        }

        for (Iterator<String> i = initializedSources.keySet().iterator(); i.hasNext();) {
            String key = i.next();
            if (!order.contains(key)) {
                if (debug) {
                    console.sendMessage(LogUtil.HEADING + "<c9>Source " + key + " was not provided in the source order. Removing.</c9>");
                }
                sourceManager.deregisterSource(key);
                i.remove();
            }
        }

        for (int i = 0; i < order.size(); i++) {
            String s = order.get(i);
            Source<? extends SourceModel> source = initializedSources.get(s);
            sourceManager.deregisterSource(s);
            sourceManager.registerSource(source, i);
            if (debug) {
                console.sendMessage(LogUtil.HEADING + "<c2>Added/Replaced source:</c2> <c1>" + s + " (" + source.getClass().getSimpleName() + ")</c1>");
            }
        }
    }

    private static @NonNull CommentedConfigurationNode getConfig(@NonNull String resourcePath, @NonNull File fileOnDisk) throws IOException {
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
            try (InputStream inStream = ConfigurationFileUtil.class.getResourceAsStream("/" + resourcePath)) {
                if (inStream != null) {
                    try (FileOutputStream outStream = new FileOutputStream(fileOnDisk)) {
                        int read;
                        byte[] buffer = new byte[4096];
                        while ((read = inStream.read(buffer, 0, buffer.length)) > 0) {
                            outStream.write(buffer, 0, read);
                        }
                    }
                }
            }
        }

        ConfigurationLoader<CommentedConfigurationNode> loader = YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK).indent(2).file(fileOnDisk).build();
        CommentedConfigurationNode config = loader.load(ConfigurationOptions.defaults().header("Comments are gone because update :(. Click here for new config + comments: https://www.spigotmc.org/resources/anti-vpn.58291/"));
        ConfigurationVersionUtil.conformVersion(loader, config, fileOnDisk);

        return config;
    }

    private static class AddressPort {
        private final String address;
        private final int port;

        public AddressPort(@NonNull String node, @NonNull String raw, int defaultPort) {
            String a = raw;
            int portIndex = a.indexOf(':');
            int p;
            if (portIndex > -1) {
                p = Integer.parseInt(a.substring(portIndex + 1));
                a = a.substring(0, portIndex);
            } else {
                logger.warn(node + " port is an unknown value. Using default value.");
                p = defaultPort;
            }

            this.address = a;
            this.port = p;
        }

        public @NonNull String getAddress() { return address; }

        public int getPort() { return port; }
    }

    private static class PoolSettings {
        private final int minPoolSize;
        private final int maxPoolSize;
        private final long maxLifetime;
        private final long timeout;

        public PoolSettings(ConfigurationNode settingsNode) {
            minPoolSize = settingsNode.node("min-idle").getInt();
            maxPoolSize = settingsNode.node("max-pool-size").getInt();
            maxLifetime = settingsNode.node("max-lifetime").getLong();
            timeout = settingsNode.node("timeout").getLong();
        }

        public int getMinPoolSize() { return minPoolSize; }

        public int getMaxPoolSize() { return maxPoolSize; }

        public long getMaxLifetime() { return maxLifetime; }

        public long getTimeout() { return timeout; }
    }
}

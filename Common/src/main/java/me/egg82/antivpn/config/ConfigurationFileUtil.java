package me.egg82.antivpn.config;

import co.aikar.commands.CommandIssuer;
import com.google.common.reflect.TypeToken;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.apis.SourceAPI;
import me.egg82.antivpn.config.enums.VPNAlgorithmMethod;
import me.egg82.antivpn.messaging.*;
import me.egg82.antivpn.storage.MySQLStorageService;
import me.egg82.antivpn.storage.SQLiteStorageService;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.PacketUtil;
import me.egg82.antivpn.utils.TimeUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.reflect.PackageFilter;
import ninja.egg82.service.ServiceLocator;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import redis.clients.jedis.exceptions.JedisException;

public class ConfigurationFileUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationFileUtil.class);

    private ConfigurationFileUtil() {}

    public static void reloadConfig(File dataDirectory, CommandIssuer console, MessagingHandler messagingHandler) {
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

        Map<String, SourceAPI> sources = getAllSources(debug);
        Set<String> stringSources;
        try {
            stringSources = new LinkedHashSet<>(config.getNode("sources", "order").getList(TypeToken.of(String.class)));
        } catch (ObjectMappingException ex) {
            logger.error(ex.getMessage(), ex);
            stringSources = new LinkedHashSet<>();
        }

        for (Iterator<String> i = stringSources.iterator(); i.hasNext();) {
            String source = i.next();
            if (!config.getNode("sources", source, "enabled").getBoolean()) {
                if (debug) {
                    logger.info(LogUtil.getHeading() + ChatColor.DARK_RED + source + " is disabled. Removing.");
                }
                i.remove();
                continue;
            }

            Optional<SourceAPI> api = getAPI(source, sources);
            if (api.isPresent() && api.get().isKeyRequired() && config.getNode("sources", source, "key").getString("").isEmpty()) {
                if (debug) {
                    logger.info(LogUtil.getHeading() + ChatColor.DARK_RED + source + " requires a key which was not provided. Removing.");
                }
                i.remove();
            }
        }
        for(Iterator<Map.Entry<String, SourceAPI>> i = sources.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<String, SourceAPI> kvp = i.next();
            if (!stringSources.contains(kvp.getKey())) {
                if (debug) {
                    logger.info(LogUtil.getHeading() + ChatColor.DARK_RED + "Removed undefined source: " + ChatColor.WHITE + kvp.getKey());
                }
                i.remove();
            }
        }

        if (debug) {
            for (String source : stringSources) {
                logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "Added source: " + ChatColor.WHITE + source);
            }
        }

        Optional<TimeUtil.Time> sourceCacheTime = TimeUtil.getTime(config.getNode("sources", "cache-time").getString("6hours"));
        if (!sourceCacheTime.isPresent()) {
            logger.warn("sources.cache-time is not a valid time pattern. Using default value.");
            sourceCacheTime = Optional.of(new TimeUtil.Time(6L, TimeUnit.HOURS));
        }

        if (debug) {
            logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "Source cache time: " + ChatColor.WHITE + sourceCacheTime.get().getMillis() + "ms");
        }

        Optional<TimeUtil.Time> mcleaksCacheTime = TimeUtil.getTime(config.getNode("mcleaks", "cache-time").getString("1day"));
        if (!mcleaksCacheTime.isPresent()) {
            logger.warn("mcleaks.cache-time is not a valid time pattern. Using default value.");
            mcleaksCacheTime = Optional.of(new TimeUtil.Time(1L, TimeUnit.DAYS));
        }

        if (debug) {
            logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "MCLeaks cache time: " + ChatColor.WHITE + mcleaksCacheTime.get().getMillis() + "ms");
        }

        Set<String> ignoredIps;
        try {
            ignoredIps = new HashSet<>(config.getNode("action", "ignore").getList(TypeToken.of(String.class)));
        } catch (ObjectMappingException ex) {
            logger.error(ex.getMessage(), ex);
            ignoredIps = new HashSet<>();
        }
        for (Iterator<String> i = ignoredIps.iterator(); i.hasNext();) {
            String ip = i.next();
            if (!ValidationUtil.isValidIp(ip) && !ValidationUtil.isValidIPRange(ip)) {
                if (debug) {
                    logger.info(LogUtil.getHeading() + ChatColor.DARK_RED + "Removed invalid ignore IP/range: " + ChatColor.WHITE + ip);
                }
                i.remove();
            }
        }

        if (debug) {
            for (String ip : ignoredIps) {
                logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "Adding ignored IP or range: " + ChatColor.WHITE + ip);
            }
        }

        Optional<TimeUtil.Time> cacheTime = TimeUtil.getTime(config.getNode("connection", "cache-time").getString("1minute"));
        if (!cacheTime.isPresent()) {
            logger.warn("connection.cache-time is not a valid time pattern. Using default value.");
            cacheTime = Optional.of(new TimeUtil.Time(1L, TimeUnit.MINUTES));
        }

        if (debug) {
            logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "Memory cache time: " + ChatColor.WHITE + cacheTime.get().getMillis() + "ms");
        }

        List<String> vpnActionCommands;
        try {
            vpnActionCommands = new ArrayList<>(config.getNode("action", "vpn", "commands").getList(TypeToken.of(String.class)));
        } catch (ObjectMappingException ex) {
            logger.error(ex.getMessage(), ex);
            vpnActionCommands = new ArrayList<>();
        }
        vpnActionCommands.removeIf(action -> action == null || action.isEmpty());

        if (debug) {
            for (String action : vpnActionCommands) {
                logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "Including command action for VPN usage: " + ChatColor.WHITE + action);
            }
        }

        List<String> mcleaksActionCommands;
        try {
            mcleaksActionCommands = new ArrayList<>(config.getNode("action", "mcleaks", "commands").getList(TypeToken.of(String.class)));
        } catch (ObjectMappingException ex) {
            logger.error(ex.getMessage(), ex);
            mcleaksActionCommands = new ArrayList<>();
        }
        mcleaksActionCommands.removeIf(action -> action == null || action.isEmpty());

        if (debug) {
            for (String action : mcleaksActionCommands) {
                logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "Including command action for MCLeaks usage: " + ChatColor.WHITE + action);
            }
        }

        VPNAlgorithmMethod vpnAlgorithmMethod = VPNAlgorithmMethod.getByName(config.getNode("action", "vpn", "algorithm", "method").getString("cascade"));
        if (vpnAlgorithmMethod == null) {
            logger.warn("action.vpn.algorithm.method is not a valid type. Using default value.");
            vpnAlgorithmMethod = VPNAlgorithmMethod.CASCADE;
        }

        double vpnAlgorithmConsensus = config.getNode("action", "vpn", "algorithm", "min-consensus").getDouble(0.6d);
        vpnAlgorithmConsensus = Math.max(0.0d, Math.min(1.0d, vpnAlgorithmConsensus));

        CachedConfig cachedConfig = CachedConfig.builder()
                .debug(debug)
                .language(getLanguage(config, debug, console))
                .storage(getStorage(config, dataDirectory, debug, console))
                .messaging(getMessaging(config, serverId, messagingHandler, debug, console))
                .sources(getSources(config, debug, console))
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
                .vpnAlgorithmMethod(getVpnAlgorithmMethod(config, debug, console))
                .vpnAlgorithmConsensus(getVpnAlgorithmConsensus(config, debug, console))
                .build();

        PacketUtil.setPoolSize(cachedConfig.getMessaging().size());

        if (cachedConfig.getStorage().isEmpty()) {
            logger.error("AntiVPN requires at least one storage service be enabled and configured.");
        }

        ConfigUtil.setConfiguration(config, cachedConfig);

        ServiceLocator.register(config);
        ServiceLocator.register(cachedValues);

        VPNAPI.reload();

        if (debug) {
            logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "API threads: " + ChatColor.WHITE + cachedValues.getThreads());
            logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "API timeout: " + ChatColor.WHITE + cachedValues.getTimeout() + "ms");
        }
    }

    private static Locale getLanguage(ConfigurationNode config, boolean debug, CommandIssuer console) {
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

    private static List<MessagingService> getMessaging(ConfigurationNode config, UUID serverId, MessagingHandler handler, boolean debug, CommandIssuer console) {
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

    private static MessagingService getMessagingOf(String name, ConfigurationNode engineNode, UUID serverId, MessagingHandler handler, PoolSettings poolSettings, boolean debug, CommandIssuer console) {
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

    private static List<StorageService> getStorage(ConfigurationNode config, File dataDirectory, boolean debug, CommandIssuer console) {
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

    private static StorageService getStorageOf(String name, ConfigurationNode engineNode, File dataDirectory, PoolSettings poolSettings, boolean debug, CommandIssuer console) {
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
                    console.sendMessage(LogUtil.HEADING + "<c2>Setting options for engine</c2> <c1>" + name + "</c1> <c2>to</c2> <c1>" + options + "</c1>");
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
                    console.sendMessage(LogUtil.HEADING + "<c2>Setting options for engine</c2> <c1>" + name + "</c1> <c2>to</c2> <c1>" + options + "</c1>");
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

    private static SourceAPI getAPI(String name, Map<String, SourceAPI> sources) { return sources.getOrDefault(name, null); }

    private static Map<String, SourceAPI> getAllSources(boolean debug) {
        List<Class<SourceAPI>> sourceClasses = PackageFilter.getClasses(SourceAPI.class, "me.egg82.antivpn.apis.vpn", false, false, false);
        Map<String, SourceAPI> retVal = new HashMap<>();
        for (Class<SourceAPI> clazz : sourceClasses) {
            if (debug) {
                logger.info("Initializing VPN API " + clazz.getName());
            }

            try {
                SourceAPI api = clazz.newInstance();
                retVal.put(api.getName(), api);
            } catch (InstantiationException | IllegalAccessException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        return retVal;
    }

    private static CommentedConfigurationNode getConfig(String resourcePath, File fileOnDisk) throws IOException {
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
            try (InputStreamReader reader = new InputStreamReader(ConfigurationFileUtil.class.getResourceAsStream("/" + resourcePath));
                 BufferedReader in = new BufferedReader(reader);
                 FileWriter writer = new FileWriter(fileOnDisk);
                 BufferedWriter out = new BufferedWriter(writer)) {
                String line;
                while ((line = in.readLine()) != null) {
                    out.write(line + System.lineSeparator());
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

        public AddressPort(String node, String raw, int defaultPort) {
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

        public String getAddress() { return address; }

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

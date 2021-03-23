package me.egg82.antivpn.config;

import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.locale.I18NManager;
import me.egg82.antivpn.locale.LocaleUtil;
import me.egg82.antivpn.locale.LocalizedCommandSender;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.logging.GELFLoggerUtil;
import me.egg82.antivpn.messaging.*;
import me.egg82.antivpn.messaging.handler.MessagingHandler;
import me.egg82.antivpn.reflect.PackageFilter;
import me.egg82.antivpn.storage.*;
import me.egg82.antivpn.utils.PacketUtil;
import me.egg82.antivpn.utils.TimeUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ConfigurationFileUtil {
    private static final @NotNull Logger logger = new GELFLogger(LoggerFactory.getLogger(ConfigurationFileUtil.class));

    private ConfigurationFileUtil() { }

    public static boolean getAllowErrorStats(@NotNull File dataDirectory) {
        ConfigurationNode config;
        try {
            config = getConfigSimple("config.yml", new File(dataDirectory, "config.yml"), null);
        } catch (IOException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            return false;
        }

        return config.node("stats", "errors").getBoolean(true);
    }

    @NotNull
    public static <M extends LocalizedCommandSender<M, B>, B> Locale getConsoleLocale(
            @NotNull File dataDirectory,
            @NotNull LocalizedCommandSender<M, B> console
    ) {
        ConfigurationNode config;
        try {
            config = getConfigSimple("config.yml", new File(dataDirectory, "config.yml"), console);
        } catch (IOException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            return Locale.US;
        }

        Locale retVal = getLanguage(config, false, console);
        LocaleUtil.setLocale(I18NManager.getManager(dataDirectory, retVal));
        return retVal;
    }

    public static <M extends LocalizedCommandSender<M, B>, B> void reloadConfig(
            @NotNull File dataDirectory,
            @NotNull LocalizedCommandSender<M, B> console,
            @NotNull MessagingHandler messagingHandler,
            @NotNull SourceManager sourceManager
    ) {
        ConfigurationNode config;
        try {
            config = getConfig("config.yml", new File(dataDirectory, "config.yml"), console);
        } catch (IOException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            return;
        }

        GELFLoggerUtil.doSendErrors(config.node("stats", "errors").getBoolean(true));

        boolean debug = config.node("debug").getBoolean(false);
        if (debug) {
            console.sendMessage(MessageKey.CONFIG__DEBUG);
        }

        Locale language = getLanguage(config, debug, console);
        LocaleUtil.setLocale(I18NManager.getManager(dataDirectory, language));

        UUID serverId = ServerIDUtil.getId(new File(dataDirectory, "server-id.txt"));
        if (debug) {
            console.sendMessage(MessageKey.CONFIG__SERVER_ID, "{id}", serverId.toString());
        }

        AlgorithmMethod vpnAlgorithmMethod = getVpnAlgorithmMethod(config, debug, console);

        CachedConfig cachedConfig = CachedConfig.builder()
                .debug(debug)
                .language(language)
                .storage(getStorage(config, dataDirectory, debug, console))
                .messaging(getMessaging(config, serverId, messagingHandler, new File(dataDirectory, "packets"), debug, console))
                .sourceCacheTime(getSourceCacheTime(config, debug, console))
                .mcleaksCacheTime(getMcLeaksCacheTime(config, debug, console))
                .cacheTime(getCacheTime(config, debug, console))
                .ignoredIps(getIgnoredIps(config, debug, console))
                .threads(config.node("connection", "threads").getInt(4))
                .timeout(config.node("connection", "timeout").getLong(5000L))
                .vpnKickMessage(config.node("action", "vpn", "kick-message").getString("<red>Please disconnect from your proxy or VPN before re-joining!</red>"))
                .vpnActionCommands(getVpnActionCommands(config, debug, console))
                .mcleaksKickMessage(config.node("action", "mcleaks", "kick-message").getString("<red>Please discontinue your use of an MCLeaks account!</red>"))
                .mcleaksActionCommands(getMcLeaksActionCommands(config, debug, console))
                .vpnAlgorithmMethod(vpnAlgorithmMethod)
                .vpnAlgorithmConsensus(getVpnAlgorithmConsensus(config, vpnAlgorithmMethod == AlgorithmMethod.CONSESNSUS, debug, console))
                .mcleaksKey(config.node("mcleaks", "key").getString(""))
                .adminPermissionNode(config.node("permissions", "admin").getString("avpn.admin"))
                .bypassPermissionNode(config.node("permissions", "bypass").getString("avpn.bypass"))
                .serverId(serverId)
                .build();

        PacketUtil.setPoolSize(cachedConfig.getMessaging().size() + 1);

        HiddenConfig hiddenConfig = HiddenConfig.builder()
                .doPacketDump(config.node("debug", "packet-dump").getBoolean(false))
                .build();

        ConfigUtil.setConfiguration(config, cachedConfig, hiddenConfig);

        setSources(config, debug, console, sourceManager);

        if (debug) {
            console.sendMessage(MessageKey.CONFIG__SOURCE_THREADS, "{threads}", String.valueOf(cachedConfig.getThreads()));
            console.sendMessage(MessageKey.CONFIG__SOURCE_TIMEOUT, "{timeout}", String.valueOf(cachedConfig.getTimeout()));
            console.sendMessage(MessageKey.CONFIG__VPN_KICK_MESSAGE, "{message}", cachedConfig.getVPNKickMessage());
            console.sendMessage(MessageKey.CONFIG__MCLEAKS_KICK_MESSAGE, "{message}", cachedConfig.getMCLeaksKickMessage());
            if (!cachedConfig.getMcLeaksKey().isEmpty()) {
                console.sendMessage(MessageKey.CONFIG__MCLEAKS_KEY);
            }
            console.sendMessage(MessageKey.CONFIG__ADMIN_NODE, "{node}", cachedConfig.getAdminPermissionNode());
            console.sendMessage(MessageKey.CONFIG__BYPASS_NODE, "{node}", cachedConfig.getBypassPermissionNode());
        }
    }

    @NotNull
    private static <M extends LocalizedCommandSender<M, B>, B> Locale getLanguage(
            @NotNull ConfigurationNode config,
            boolean debug,
            @NotNull LocalizedCommandSender<M, B> console
    ) {
        String configLanguage = config.node("lang").getString("en-US");
        Locale retVal = null;
        for (Locale locale : Locale.getAvailableLocales()) {
            String l = locale.getCountry() == null || locale.getCountry().isEmpty() ? locale.getLanguage() : locale.getLanguage() + "-" + locale.getCountry();
            if (locale.getLanguage().equalsIgnoreCase(configLanguage) || l.equalsIgnoreCase(configLanguage)) {
                retVal = locale;
                break;
            }
            if (locale.getCountry() != null && !locale.getCountry().isEmpty()) {
                l = locale.getLanguage() + "_" + locale.getCountry();
                if (locale.getLanguage().equalsIgnoreCase(configLanguage) || l.equalsIgnoreCase(configLanguage)) {
                    retVal = locale;
                    break;
                }
            }
        }

        if (retVal == null) {
            retVal = Locale.US;
            console.sendMessage(
                    MessageKey.CONFIG__INVALID_LOCALE,
                    "{lang}",
                    configLanguage,
                    "{default}",
                    retVal.getCountry() == null || retVal.getCountry().isEmpty() ? retVal.getLanguage() : retVal.getLanguage() + "-" + retVal.getCountry()
            );
        }
        if (debug) {
            console.sendMessage(
                    MessageKey.CONFIG__CONSOLE_LOCALE,
                    "{lang}",
                    retVal.getCountry() == null || retVal.getCountry().isEmpty() ? retVal.getLanguage() : retVal.getLanguage() + "-" + retVal.getCountry()
            );
        }

        return retVal;
    }

    @NotNull
    private static <M extends LocalizedCommandSender<M, B>, B> List<@NotNull StorageService> getStorage(
            @NotNull ConfigurationNode config,
            @NotNull File dataDirectory,
            boolean debug,
            @NotNull LocalizedCommandSender<M, B> console
    ) {
        List<@NotNull StorageService> retVal = new ArrayList<>();

        PoolSettings poolSettings = new PoolSettings(config.node("storage", "settings"));
        for (Map.Entry<Object, ? extends ConfigurationNode> kvp : config.node("storage", "engines").childrenMap().entrySet()) {
            StorageService service = getStorageOf((String) kvp.getKey(), kvp.getValue(), dataDirectory, poolSettings, debug, console);
            if (service == null) {
                continue;
            }

            if (debug) {
                console.sendMessage("<c2>Added storage:</c2> <c1>" + service.getName() + " (" + service.getClass().getSimpleName() + ")</c1>");
            }
            retVal.add(service);
        }

        return retVal;
    }

    @Nullable
    private static <M extends LocalizedCommandSender<M, B>, B> StorageService getStorageOf(
            @NotNull String name,
            @NotNull ConfigurationNode engineNode,
            @NotNull File dataDirectory,
            @NotNull PoolSettings poolSettings,
            boolean debug,
            @NotNull LocalizedCommandSender<M, B> console
    ) {
        if (!engineNode.node("enabled").getBoolean()) {
            if (debug) {
                console.sendMessage("<c9>Storage engine</c9> <c1>" + name + "</c1> <c9>is disabled. Removing.</c9>");
            }
            return null;
        }

        String type = engineNode.node("type").getString("").toLowerCase();
        ConfigurationNode connectionNode = engineNode.node("connection");
        switch (type) {
            case "old_mysql": {
                AddressPort url = new AddressPort(connectionNode.key() + ".address", connectionNode.node("address").getString("127.0.0.1:3306"), 3306, console);
                if (debug) {
                    console.sendMessage("<c2>Creating engine</c2> <c1>" + name + "</c1> <c2>of type old_mysql with address</c2> <c1>" + url.getAddress() + ":" + url.getPort() + "/" + connectionNode
                            .node("database")
                            .getString("anti_vpn") + "</c1>");
                }
                String options = connectionNode.node("options").getString("useSSL=false&useUnicode=true&characterEncoding=utf8");
                if (options.length() > 0 && options.charAt(0) == '?') {
                    options = options.substring(1);
                }
                if (debug) {
                    console.sendMessage("<c2>Setting options for engine</c2> <c1>" + name + "</c1> <c2>to</c2> <c1>" + options.replace("&", "&\\") + "</c1>");
                }
                try {
                    return MySQL55StorageService.builder(name)
                            .url(url.address, url.port, connectionNode.node("database").getString("anti_vpn"))
                            .credentials(connectionNode.node("username").getString(""), connectionNode.node("password").getString(""))
                            .options(options)
                            .poolSize(poolSettings.minPoolSize, poolSettings.maxPoolSize)
                            .life(poolSettings.maxLifetime, poolSettings.timeout)
                            .build();
                } catch (Exception ex) {
                    logger.error(console.getLocalizedText(MessageKey.ERROR__CONFIG__NO_ENGINE, "{name}", name), ex);
                }
                break;
            }
            case "mysql": {
                AddressPort url = new AddressPort(connectionNode.key() + ".address", connectionNode.node("address").getString("127.0.0.1:3306"), 3306, console);
                if (debug) {
                    console.sendMessage("<c2>Creating engine</c2> <c1>" + name + "</c1> <c2>of type mysql with address</c2> <c1>" + url.getAddress() + ":" + url.getPort() + "/" + connectionNode
                            .node("database")
                            .getString("anti_vpn") + "</c1>");
                }
                String options = connectionNode.node("options").getString("useSSL=false&useUnicode=true&characterEncoding=utf8");
                if (options.length() > 0 && options.charAt(0) == '?') {
                    options = options.substring(1);
                }
                if (debug) {
                    console.sendMessage("<c2>Setting options for engine</c2> <c1>" + name + "</c1> <c2>to</c2> <c1>" + options.replace("&", "&\\") + "</c1>");
                }
                try {
                    return MySQLStorageService.builder(name)
                            .url(url.address, url.port, connectionNode.node("database").getString("anti_vpn"))
                            .credentials(connectionNode.node("username").getString(""), connectionNode.node("password").getString(""))
                            .options(options)
                            .poolSize(poolSettings.minPoolSize, poolSettings.maxPoolSize)
                            .life(poolSettings.maxLifetime, poolSettings.timeout)
                            .build();
                } catch (Exception ex) {
                    logger.error(console.getLocalizedText(MessageKey.ERROR__CONFIG__NO_ENGINE, "{name}", name), ex);
                }
                break;
            }
            case "mariadb": {
                AddressPort url = new AddressPort(connectionNode.key() + ".address", connectionNode.node("address").getString("127.0.0.1:3306"), 3306, console);
                if (debug) {
                    console.sendMessage("<c2>Creating engine</c2> <c1>" + name + "</c1> <c2>of type mariadb with address</c2> <c1>" + url.getAddress() + ":" + url.getPort() + "/" + connectionNode
                            .node("database")
                            .getString("anti_vpn") + "</c1>");
                }
                String options = connectionNode.node("options").getString("useSSL=false&useUnicode=true&characterEncoding=utf8");
                if (options.length() > 0 && options.charAt(0) == '?') {
                    options = options.substring(1);
                }
                if (debug) {
                    console.sendMessage("<c2>Setting options for engine</c2> <c1>" + name + "</c1> <c2>to</c2> <c1>" + options.replace("&", "&\\") + "</c1>");
                }
                try {
                    return MariaDBStorageService.builder(name)
                            .url(url.address, url.port, connectionNode.node("database").getString("anti_vpn"))
                            .credentials(connectionNode.node("username").getString(""), connectionNode.node("password").getString(""))
                            .options(options)
                            .poolSize(poolSettings.minPoolSize, poolSettings.maxPoolSize)
                            .life(poolSettings.maxLifetime, poolSettings.timeout)
                            .build();
                } catch (Exception ex) {
                    logger.error(console.getLocalizedText(MessageKey.ERROR__CONFIG__NO_ENGINE, "{name}", name), ex);
                }
                break;
            }
            case "postgresql": {
                AddressPort url = new AddressPort(connectionNode.key() + ".address", connectionNode.node("address").getString("127.0.0.1:5432"), 5432, console);
                if (debug) {
                    console.sendMessage("<c2>Creating engine</c2> <c1>" + name + "</c1> <c2>of type postgresql with address</c2> <c1>" + url.getAddress() + ":" + url.getPort() + "/" + connectionNode
                            .node("database")
                            .getString("anti_vpn") + "</c1>");
                }
                String options = connectionNode.node("options").getString("useSSL=false&useUnicode=true&characterEncoding=utf8");
                if (options.length() > 0 && options.charAt(0) == '?') {
                    options = options.substring(1);
                }
                if (debug) {
                    console.sendMessage("<c2>Setting options for engine</c2> <c1>" + name + "</c1> <c2>to</c2> <c1>" + options.replace("&", "&\\") + "</c1>");
                }
                try {
                    return PostgreSQLStorageService.builder(name)
                            .url(url.address, url.port, connectionNode.node("database").getString("anti_vpn"))
                            .credentials(connectionNode.node("username").getString(""), connectionNode.node("password").getString(""))
                            .options(options)
                            .poolSize(poolSettings.minPoolSize, poolSettings.maxPoolSize)
                            .life(poolSettings.maxLifetime, poolSettings.timeout)
                            .build();
                } catch (Exception ex) {
                    logger.error(console.getLocalizedText(MessageKey.ERROR__CONFIG__NO_ENGINE, "{name}", name), ex);
                }
                break;
            }
            case "h2": {
                if (debug) {
                    console.sendMessage("<c2>Creating engine</c2> <c1>" + name + "</c1> <c2>of type h2 with file</c2> <c1>" + connectionNode.node("file")
                            .getString("anti_vpn") + "</c1>");
                }
                String options = connectionNode.node("options").getString("useUnicode=true&characterEncoding=utf8");
                if (options.length() > 0 && options.charAt(0) == '?') {
                    options = options.substring(1);
                }
                if (debug) {
                    console.sendMessage("<c2>Setting options for engine</c2> <c1>" + name + "</c1> <c2>to</c2> <c1>" + options.replace("&", "&\\") + "</c1>");
                }
                try {
                    return H2StorageService.builder(name)
                            .file(new File(dataDirectory, connectionNode.node("file").getString("anti_vpn")))
                            .options(options)
                            .poolSize(poolSettings.minPoolSize, poolSettings.maxPoolSize)
                            .life(poolSettings.maxLifetime, poolSettings.timeout)
                            .build();
                } catch (Exception ex) {
                    logger.error(console.getLocalizedText(MessageKey.ERROR__CONFIG__NO_ENGINE, "{name}", name), ex);
                }
                break;
            }
            case "sqlite": {
                if (debug) {
                    console.sendMessage("<c2>Creating engine</c2> <c1>" + name + "</c1> <c2>of type sqlite with file</c2> <c1>" + connectionNode.node("file")
                            .getString("anti_vpn.db") + "</c1>");
                }
                String options = connectionNode.node("options").getString("useUnicode=true&characterEncoding=utf8");
                if (options.length() > 0 && options.charAt(0) == '?') {
                    options = options.substring(1);
                }
                if (debug) {
                    console.sendMessage("<c2>Setting options for engine</c2> <c1>" + name + "</c1> <c2>to</c2> <c1>" + options.replace("&", "&\\") + "</c1>");
                }
                try {
                    return SQLiteStorageService.builder(name)
                            .file(new File(dataDirectory, connectionNode.node("file").getString("anti_vpn.db")))
                            .options(options)
                            .poolSize(poolSettings.minPoolSize, poolSettings.maxPoolSize)
                            .life(poolSettings.maxLifetime, poolSettings.timeout)
                            .build();
                } catch (Exception ex) {
                    logger.error(console.getLocalizedText(MessageKey.ERROR__CONFIG__NO_ENGINE, "{name}", name), ex);
                }
                break;
            }
            default: {
                console.sendMessage("<c9>Unknown storage type</c9> <c1>" + type + "</c1> <c9>in engine</c9> <c1>" + name + "</c1>");
                break;
            }
        }
        return null;
    }

    @NotNull
    private static <M extends LocalizedCommandSender<M, B>, B> List<@NotNull MessagingService> getMessaging(
            @NotNull ConfigurationNode config,
            @NotNull UUID serverId,
            @NotNull MessagingHandler handler,
            @NotNull File packetDirectory,
            boolean debug,
            @NotNull LocalizedCommandSender<M, B> console
    ) {
        List<@NotNull MessagingService> retVal = new ArrayList<>();

        PoolSettings poolSettings = new PoolSettings(config.node("messaging", "settings"));
        for (Map.Entry<Object, ? extends ConfigurationNode> kvp : config.node("messaging", "engines").childrenMap().entrySet()) {
            MessagingService service = getMessagingOf((String) kvp.getKey(), kvp.getValue(), serverId, handler, packetDirectory, poolSettings, debug, console);
            if (service == null) {
                continue;
            }

            if (debug) {
                console.sendMessage("<c2>Added messaging:</c2> <c1>" + service.getName() + " (" + service.getClass().getSimpleName() + ")</c1>");
            }
            retVal.add(service);
        }

        return retVal;
    }

    @Nullable
    private static <M extends LocalizedCommandSender<M, B>, B> MessagingService getMessagingOf(
            @NotNull String name,
            @NotNull ConfigurationNode engineNode,
            @NotNull UUID serverId,
            @NotNull MessagingHandler handler,
            @NotNull File packetDirectory,
            @NotNull PoolSettings poolSettings,
            boolean debug,
            @NotNull LocalizedCommandSender<M, B> console
    ) {
        if (!engineNode.node("enabled").getBoolean()) {
            if (debug) {
                console.sendMessage("<c9>Messaging engine</c9> <c1>" + name + "</c1> <c9>is disabled. Removing.</c9>");
            }
            return null;
        }

        String type = engineNode.node("type").getString("").toLowerCase();
        ConfigurationNode connectionNode = engineNode.node("connection");
        switch (type) {
            case "rabbitmq": {
                AddressPort url = new AddressPort(connectionNode.key() + ".address", connectionNode.node("address").getString("127.0.0.1:5672"), 5672, console);
                if (debug) {
                    console.sendMessage("<c2>Creating engine</c2> <c1>" + name + "</c1> <c2>of type rabbitmq with address</c2> <c1>" + url.getAddress() + ":" + url.getPort() + connectionNode
                            .node("v-host")
                            .getString("/") + "</c1>");
                }
                try {
                    return RabbitMQMessagingService.builder(name, serverId, handler, packetDirectory)
                            .url(url.address, url.port, connectionNode.node("v-host").getString("/"))
                            .credentials(connectionNode.node("username").getString("guest"), connectionNode.node("password").getString("guest"))
                            .timeout((int) poolSettings.timeout)
                            .build();
                } catch (IOException | TimeoutException ex) {
                    logger.error(console.getLocalizedText(MessageKey.ERROR__CONFIG__NO_ENGINE, "{name}", name), ex);
                }
                break;
            }
            case "redis": {
                AddressPort url = new AddressPort(connectionNode.key() + ".address", connectionNode.node("address").getString("127.0.0.1:6379"), 6379, console);
                if (debug) {
                    console.sendMessage("<c2>Creating engine</c2> <c1>" + name + "</c1> <c2>of type redis with address</c2> <c1>" + url.getAddress() + ":" + url.getPort() + "</c1>");
                }
                try {
                    return RedisMessagingService.builder(name, serverId, handler, packetDirectory)
                            .url(url.address, url.port)
                            .credentials(connectionNode.node("password").getString(""))
                            .poolSize(poolSettings.minPoolSize, poolSettings.maxPoolSize)
                            .life(poolSettings.maxLifetime, (int) poolSettings.timeout)
                            .build();
                } catch (JedisException ex) {
                    logger.error(console.getLocalizedText(MessageKey.ERROR__CONFIG__NO_ENGINE, "{name}", name), ex);
                }
                break;
            }
            case "nats": {
                AddressPort url = new AddressPort(connectionNode.key() + ".address", connectionNode.node("address").getString("127.0.0.1:4222"), 4222, console);
                if (debug) {
                    console.sendMessage("<c2>Creating engine</c2> <c1>" + name + "</c1> <c2>of type NATS with address</c2> <c1>" + url.getAddress() + ":" + url.getPort() + "</c1>");
                }
                try {
                    return NATSMessagingService.builder(name, serverId, handler, packetDirectory)
                            .url(url.address, url.port)
                            .credentials(connectionNode.node("file").getString(""))
                            .life((int) poolSettings.timeout)
                            .build();
                } catch (InterruptedException ex) {
                    logger.error(console.getLocalizedText(MessageKey.ERROR__CONFIG__NO_ENGINE, "{name}", name), ex);
                    Thread.currentThread().interrupt();
                } catch (IOException | IllegalStateException ex) {
                    logger.error(console.getLocalizedText(MessageKey.ERROR__CONFIG__NO_ENGINE, "{name}", name), ex);
                }
                break;
            }
            default: {
                console.sendMessage("<c9>Unknown messaging type</c9> <c1>" + type + "</c1> <c9>in engine</c9> <c1>" + name + "</c1>");
                break;
            }
        }
        return null;
    }

    @NotNull
    private static <M extends LocalizedCommandSender<M, B>, B> TimeUtil.Time getSourceCacheTime(
            @NotNull ConfigurationNode config,
            boolean debug,
            @NotNull LocalizedCommandSender<M, B> console
    ) {
        TimeUtil.Time retVal = TimeUtil.getTime(config.node("sources", "cache-time").getString("6hours"));
        if (retVal == null) {
            console.sendMessage("<c2>sources.cache-time is not a valid time pattern. Using default value.<c2>");
            retVal = new TimeUtil.Time(6L, TimeUnit.HOURS);
        }

        if (debug) {
            console.sendMessage("<c2>Source cache time:</c2> <c1>" + retVal.getMillis() + "ms (" + retVal.getTime() + " " + retVal.getUnit().name() + ")</c1>");
        }
        return retVal;
    }

    @NotNull
    private static <M extends LocalizedCommandSender<M, B>, B> TimeUtil.Time getMcLeaksCacheTime(
            @NotNull ConfigurationNode config,
            boolean debug,
            @NotNull LocalizedCommandSender<M, B> console
    ) {
        TimeUtil.Time retVal = TimeUtil.getTime(config.node("mcleaks", "cache-time").getString("1day"));
        if (retVal == null) {
            console.sendMessage("<c2>mcleaks.cache-time is not a valid time pattern. Using default value.<c2>");
            retVal = new TimeUtil.Time(1L, TimeUnit.DAYS);
        }

        if (debug) {
            console.sendMessage("<c2>MCLeaks cache time:</c2> <c1>" + retVal.getMillis() + "ms (" + retVal.getTime() + " " + retVal.getUnit().name() + ")</c1>");
        }
        return retVal;
    }

    @NotNull
    private static <M extends LocalizedCommandSender<M, B>, B> TimeUtil.Time getCacheTime(
            @NotNull ConfigurationNode config,
            boolean debug,
            @NotNull LocalizedCommandSender<M, B> console
    ) {
        TimeUtil.Time retVal = TimeUtil.getTime(config.node("connection", "cache-time").getString("1minute"));
        if (retVal == null) {
            console.sendMessage("<c2>connection.cache-time is not a valid time pattern. Using default value.<c2>");
            retVal = new TimeUtil.Time(1L, TimeUnit.MINUTES);
        }

        if (debug) {
            console.sendMessage("<c2>Memory cache time:</c2> <c1>" + retVal.getMillis() + "ms (" + retVal.getTime() + " " + retVal.getUnit().name() + ")</c1>");
        }
        return retVal;
    }

    @NotNull
    private static <M extends LocalizedCommandSender<M, B>, B> Set<@NotNull String> getIgnoredIps(
            @NotNull ConfigurationNode config,
            boolean debug,
            @NotNull LocalizedCommandSender<M, B> console
    ) {
        Set<String> retVal;
        try {
            retVal = new HashSet<>(!config.node("action", "ignore").empty() ? config.node("action", "ignore").getList(String.class) : new ArrayList<>());
        } catch (SerializationException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            retVal = new HashSet<>();
        }

        for (Iterator<String> i = retVal.iterator(); i.hasNext(); ) {
            String ip = i.next();
            if (!ValidationUtil.isValidIp(ip) && !ValidationUtil.isValidIpRange(ip)) {
                if (debug) {
                    console.sendMessage("<c9>Removed invalid ignore IP/range:</c9> <c1>" + ip + "</c1>");
                }
                i.remove();
            } else {
                if (debug) {
                    console.sendMessage("<c2>Adding ignored IP or range:</c2> <c1>" + ip + "</c1>");
                }
            }
        }

        return retVal;
    }

    @NotNull
    private static <M extends LocalizedCommandSender<M, B>, B> Set<@NotNull String> getVpnActionCommands(
            @NotNull ConfigurationNode config,
            boolean debug,
            @NotNull LocalizedCommandSender<M, B> console
    ) {
        Set<String> retVal;
        try {
            retVal = new HashSet<>(!config.node("action", "vpn", "commands").empty()
                                   ? config.node("action", "vpn", "commands").getList(String.class)
                                   : new ArrayList<>());
        } catch (SerializationException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            retVal = new HashSet<>();
        }
        retVal.removeIf(action -> action == null || action.isEmpty());

        if (debug) {
            for (String action : retVal) {
                console.sendMessage("<c2>Adding command action for VPN usage:</c2> <c1>" + action + "</c1>");
            }
        }

        return retVal;
    }

    @NotNull
    private static <M extends LocalizedCommandSender<M, B>, B> Set<@NotNull String> getMcLeaksActionCommands(
            @NotNull ConfigurationNode config,
            boolean debug,
            @NotNull LocalizedCommandSender<M, B> console
    ) {
        Set<String> retVal;
        try {
            retVal = new HashSet<>(!config.node("action", "mcleaks", "commands").empty()
                                   ? config.node("action", "mcleaks", "commands").getList(String.class)
                                   : new ArrayList<>());
        } catch (SerializationException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            retVal = new HashSet<>();
        }
        retVal.removeIf(action -> action == null || action.isEmpty());

        if (debug) {
            for (String action : retVal) {
                console.sendMessage("<c2>Adding command action for MCLeaks usage:</c2> <c1>" + action + "</c1>");
            }
        }

        return retVal;
    }

    private static <M extends LocalizedCommandSender<M, B>, B> @NotNull AlgorithmMethod getVpnAlgorithmMethod(
            @NotNull ConfigurationNode config,
            boolean debug,
            @NotNull LocalizedCommandSender<M, B> console
    ) {
        AlgorithmMethod retVal = AlgorithmMethod.getByName(config.node("action", "vpn", "algorithm", "method").getString("cascade"));
        if (retVal == null) {
            console.sendMessage("<c2>action.vpn.algorithm.method is not a valid type. Using default value.<c2>");
            retVal = AlgorithmMethod.CASCADE;
        }

        if (debug) {
            console.sendMessage("<c2>Using VPN algorithm:</c2> <c1>" + retVal.name() + "</c1>");
        }

        return retVal;
    }

    private static <M extends LocalizedCommandSender<M, B>, B> double getVpnAlgorithmConsensus(
            ConfigurationNode config,
            boolean consensus,
            boolean debug,
            LocalizedCommandSender<M, B> console
    ) {
        double retVal = config.node("action", "vpn", "algorithm", "min-consensus").getDouble(0.6d);
        retVal = Math.max(0.0d, Math.min(1.0d, retVal));

        if (consensus && debug) {
            console.sendMessage("<c2>Using consensus value:</c2> <c1>" + retVal + "</c1>");
        }

        return retVal;
    }

    @SuppressWarnings("unchecked")
    private static <M extends LocalizedCommandSender<M, B>, B> void setSources(
            @NotNull ConfigurationNode config,
            boolean debug,
            @NotNull LocalizedCommandSender<M, B> console,
            @NotNull SourceManager sourceManager
    ) {
        Map<String, Source<SourceModel>> initializedSources = new HashMap<>();

        List<Class<Source>> sourceClasses = PackageFilter.getClasses(Source.class, "me.egg82.antivpn.api.model.source", false, false, false);
        for (Class<Source> clazz : sourceClasses) {
            if (debug) {
                console.sendMessage("<c2>Initializing source</c2> <c1>" + clazz.getSimpleName() + "</c1>");
            }

            try {
                Source<SourceModel> source = clazz.newInstance();
                initializedSources.put(source.getName(), source);
            } catch (InstantiationException | IllegalAccessException | ClassCastException ex) {
                logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }

        List<String> order;
        try {
            order = !config.node("sources", "order").empty() ? new ArrayList<>(config.node("sources", "order").getList(String.class)) : new ArrayList<>();
        } catch (SerializationException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            order = new ArrayList<>();
        }

        for (Iterator<String> i = order.iterator(); i.hasNext(); ) {
            String s = i.next();
            if (!config.node("sources", s, "enabled").getBoolean(false)) {
                if (debug) {
                    console.sendMessage("<c9>Source " + s + " is disabled. Removing.</c9>");
                }
                sourceManager.deregisterSource(s);
                i.remove();
                continue;
            }

            Source<SourceModel> source = initializedSources.get(s);
            if (source == null) {
                if (debug) {
                    console.sendMessage("<c9>Source " + s + " was not found. Removing.</c9>");
                }
                sourceManager.deregisterSource(s);
                i.remove();
                continue;
            }

            if (source.isKeyRequired() && config.node("sources", s, "key").getString("").isEmpty()) {
                if (debug) {
                    console.sendMessage("<c9>Source " + s + " requires a key which was not provided. Removing.</c9>");
                }
                sourceManager.deregisterSource(s);
                i.remove();
            }
        }

        for (Iterator<String> i = initializedSources.keySet().iterator(); i.hasNext(); ) {
            String key = i.next();
            if (!order.contains(key)) {
                if (debug) {
                    console.sendMessage("<c9>Source " + key + " was not provided in the source order. Removing.</c9>");
                }
                sourceManager.deregisterSource(key);
                i.remove();
            }
        }

        for (int i = 0; i < order.size(); i++) {
            String s = order.get(i);
            Source<SourceModel> source = initializedSources.get(s);
            sourceManager.deregisterSource(s);
            sourceManager.registerSource(source, i);
            if (debug) {
                console.sendMessage("<c2>Added/Replaced source:</c2> <c1>" + s + " (" + source.getClass().getSimpleName() + ")</c1>");
            }
        }
    }

    @NotNull
    private static <M extends LocalizedCommandSender<M, B>, B> CommentedConfigurationNode getConfigSimple(
            @NotNull String resourcePath,
            @NotNull File fileOnDisk,
            @Nullable LocalizedCommandSender<M, B> console
    ) throws IOException {
        File parentDir = fileOnDisk.getParentFile();
        if (parentDir.exists() && !parentDir.isDirectory()) {
            Files.delete(parentDir.toPath());
        }
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__PARENT_DIR));
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
        return loader.load(ConfigurationOptions.defaults().header(LocaleUtil.getDefaultI18N().getText(MessageKey.CONFIG__COMMENTS_GONE)));
    }

    @NotNull
    private static <M extends LocalizedCommandSender<M, B>, B> CommentedConfigurationNode getConfig(
            @NotNull String resourcePath,
            @NotNull File fileOnDisk,
            @NotNull LocalizedCommandSender<M, B> console
    ) throws IOException {
        ConfigurationLoader<CommentedConfigurationNode> loader = YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK).indent(2).file(fileOnDisk).build();
        CommentedConfigurationNode config = getConfigSimple(resourcePath, fileOnDisk, console);
        ConfigurationVersionUtil.conformVersion(loader, config, fileOnDisk);
        return config;
    }

    private static class AddressPort {
        private final @NotNull String address;
        private final int port;

        public <M extends LocalizedCommandSender<M, B>, B> AddressPort(
                @NotNull String node,
                @NotNull String raw,
                int defaultPort,
                @NotNull LocalizedCommandSender<M, B> console
        ) {
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

        @NotNull
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

            TimeUtil.Time l = TimeUtil.getTime(settingsNode.node("max-lifetime").getString("30minutes"));
            if (l == null) {
                l = new TimeUtil.Time(30L, TimeUnit.MINUTES);
            }
            maxLifetime = l.getMillis();

            TimeUtil.Time t = TimeUtil.getTime(settingsNode.node("timeout").getString("5seconds"));
            if (t == null) {
                t = new TimeUtil.Time(5L, TimeUnit.SECONDS);
            }
            timeout = t.getMillis();
        }

        public int getMinPoolSize() { return minPoolSize; }

        public int getMaxPoolSize() { return maxPoolSize; }

        public long getMaxLifetime() { return maxLifetime; }

        public long getTimeout() { return timeout; }
    }
}

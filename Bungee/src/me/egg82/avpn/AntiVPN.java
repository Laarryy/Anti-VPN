package me.egg82.avpn;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiConsumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bstats.bungeecord.Metrics;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

import me.egg82.avpn.core.RedisSubscriber;
import me.egg82.avpn.debug.BungeeDebugPrinter;
import me.egg82.avpn.debug.IDebugPrinter;
import me.egg82.avpn.reflection.analytics.PlanAnalyticsHelper;
import me.egg82.avpn.sql.mysql.FetchQueueMySQLCommand;
import me.egg82.avpn.sql.sqlite.ClearDataSQLiteCommand;
import me.egg82.avpn.utils.RedisUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import ninja.egg82.analytics.exceptions.GameAnalyticsExceptionHandler;
import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.analytics.exceptions.NullExceptionHandler;
import ninja.egg82.analytics.exceptions.RollbarExceptionHandler;
import ninja.egg82.bungeecord.BasePlugin;
import ninja.egg82.bungeecord.processors.CommandProcessor;
import ninja.egg82.bungeecord.processors.EventProcessor;
import ninja.egg82.enums.BaseSQLType;
import ninja.egg82.events.CompleteEventArgs;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.enums.SenderType;
import ninja.egg82.plugin.messaging.IMessageHandler;
import ninja.egg82.plugin.messaging.RabbitMessageHandler;
import ninja.egg82.plugin.utils.PluginReflectUtil;
import ninja.egg82.sql.ISQL;
import ninja.egg82.utils.ThreadUtil;
import ninja.egg82.utils.TimeUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class AntiVPN extends BasePlugin {
    // vars
    private Metrics metrics = null;

    private int numMessages = 0;
    private int numCommands = 0;
    private int numEvents = 0;

    private ScheduledFuture<?> exceptionHandlerFuture = null;
    private IExceptionHandler exceptionHandler = null;
    private String version = null;

    private static DecimalFormat format = new DecimalFormat(".##");

    // constructor
    public AntiVPN() {
        super(58716);
    }

    // public
    public static VPNAPI getAPI() {
        return VPNAPI.getInstance();
    }

    public void onLoad() {
        super.onLoad();

        version = getDescription().getVersion();

        exceptionHandler = ServiceLocator.getService(IExceptionHandler.class);
        getLogger().setLevel(Level.WARNING);

        ServiceLocator.provideService(BungeeDebugPrinter.class);

        PluginReflectUtil.addServicesFromPackage("me.egg82.avpn.registries", true);
        PluginReflectUtil.addServicesFromPackage("me.egg82.avpn.lists", true);

        Configuration config = ConfigLoader.getConfig("config.yml", "config.yml");

        Config.debug = config.getNode("debug").getBoolean();
        if (Config.debug) {
            ServiceLocator.getService(IDebugPrinter.class).printInfo("Debug enabled");
        }
        List<String> sources = null;
        try {
            sources = config.getNode("sources", "order").getList(TypeToken.of(String.class));
        } catch (Exception ex) {
            sources = new ArrayList<String>();
        }
        Set<String> sourcesOrdered = new LinkedHashSet<String>();
        for (String source : sources) {
            if (Config.debug) {
                ServiceLocator.getService(IDebugPrinter.class).printInfo("Adding potential source " + source + ".");
            }
            sourcesOrdered.add(source);
        }
        for (Iterator<String> i = sourcesOrdered.iterator(); i.hasNext();) {
            String source = i.next();
            if (!config.getNode("sources", source, "enabled").getBoolean()) {
                if (Config.debug) {
                    ServiceLocator.getService(IDebugPrinter.class).printInfo(source + " is disabled. Removing.");
                }
                i.remove();
            }
        }
        Config.sources = ImmutableSet.copyOf(sourcesOrdered);
        try {
            Config.ignore = ImmutableSet.copyOf(config.getNode("ignore").getList(TypeToken.of(String.class)));
        } catch (Exception ex) {
            Config.ignore = ImmutableSet.of();
        }
        Config.sourceCacheTime = TimeUtil.getTime(config.getNode("sources", "cacheTime").getString("6hours"));
        if (Config.debug) {
            ServiceLocator.getService(IDebugPrinter.class).printInfo("Source cache time: " + TimeUtil.timeToHoursMinsSecs(Config.sourceCacheTime));
        }
        Config.kickMessage = config.getNode("kickMessage").getString("");
        Config.async = config.getNode("async").getBoolean();
        if (Config.debug) {
            ServiceLocator.getService(IDebugPrinter.class).printInfo((Config.async) ? "Async enabled" : "Async disabled");
        }
        Config.kick = config.getNode("kick").getBoolean(true);
        if (Config.debug) {
            ServiceLocator.getService(IDebugPrinter.class).printInfo((Config.kick) ? "Set to kick" : "Set to API-only");
        }
        Config.consensus = Math.min(1.0d, config.getNode("consensus").getDouble(-1.0d)); // Cap to 1.0
        if (Config.debug) {
            ServiceLocator.getService(IDebugPrinter.class)
                .printInfo((Config.consensus < 0.0d) ? "Using cascade algorithm" : "Using consensus algorithm, set to " + format.format(Config.consensus * 100.0d) + "%");
        }
        Config.sendUsage = config.getNode("stats", "usage").getBoolean();
        if (Config.debug) {
            ServiceLocator.getService(IDebugPrinter.class).printInfo((Config.sendUsage) ? "Sending usage stats" : "Not sending usage stats");
        }
        Config.sendErrors = config.getNode("stats", "errors").getBoolean();
        if (Config.debug) {
            ServiceLocator.getService(IDebugPrinter.class).printInfo((Config.sendErrors) ? "Sending error stats" : "Not sending error stats");
        }
        Config.checkUpdates = config.getNode("update", "check").getBoolean();
        if (Config.debug) {
            ServiceLocator.getService(IDebugPrinter.class).printInfo((Config.checkUpdates) ? "Update check enabled" : "Update check disabled");
        }
        Config.notifyUpdates = config.getNode("update", "notify").getBoolean();
        if (Config.debug) {
            ServiceLocator.getService(IDebugPrinter.class).printInfo((Config.notifyUpdates) ? "Update notifications enabled" : "Update notifications disabled");
        }
    }

    public void onEnable() {
        super.onEnable();

        PluginManager manager = getProxy().getPluginManager();

        if (manager.getPlugin("Plan") != null) {
            printInfo(ChatColor.GREEN + "Enabling support for Plan.");
            ServiceLocator.provideService(PlanAnalyticsHelper.class, false);
        } else {
            printInfo(ChatColor.YELLOW + "Plan was not found. Personal analytics support has been disabled.");
        }

        if (Config.sendErrors) {
            swapExceptionHandlers(new RollbarExceptionHandler(Config.ROLLBAR_KEY, "production", version, getServerId(), getDescription().getName()));
        }

        List<IMessageHandler> services = ServiceLocator.removeServices(IMessageHandler.class);
        for (IMessageHandler handler : services) {
            try {
                handler.close();
            } catch (Exception ex) {

            }
        }

        ThreadUtil.rename(getDescription().getName());

        Loaders.loadRedis();
        MessagingLoader.loadMessaging("avpn", null, getServerId(), SenderType.PROXY);
        Loaders.loadStorage(getDescription().getName(), getClass().getClassLoader(), getDataFolder());

        numCommands = ServiceLocator.getService(CommandProcessor.class).addHandlersFromPackage("me.egg82.avpn.commands",
            PluginReflectUtil.getCommandMapFromPackage("me.egg82.avpn.commands", false, null, "Command"), false);
        numEvents = ServiceLocator.getService(EventProcessor.class).addHandlersFromPackage("me.egg82.avpn.events");
        if (ServiceLocator.hasService(IMessageHandler.class)) {
            numMessages = ServiceLocator.getService(IMessageHandler.class).addHandlersFromPackage("me.egg82.avpn.messages");
        }

        ThreadUtil.scheduleInfinite(new Runnable() {
            public void run() {
                try (Jedis redis = RedisUtil.getRedis()) {
                    if (redis != null) {
                        redis.subscribe(new RedisSubscriber(), "avpn", "avpn-consensus");
                    }
                }
            }
        }, 5L * 1000L);

        enableMessage();

        ThreadUtil.submit(new Runnable() {
            public void run() {
                try {
                    metrics = new Metrics(ServiceLocator.getService(Plugin.class));
                } catch (Exception ex) {
                    printWarning("Could not connect to bStats.");
                    return;
                }

                metrics.addCustomChart(new Metrics.AdvancedPie("sources", () -> {
                    if (!Config.sendUsage) {
                        return null;
                    }

                    Map<String, Integer> values = new HashMap<String, Integer>();
                    for (String key : Config.sources) {
                        values.put(key, Integer.valueOf(1));
                    }
                    return values;
                }));
                metrics.addCustomChart(new Metrics.SimplePie("kick", () -> {
                    if (!Config.sendUsage) {
                        return null;
                    }

                    return String.valueOf(Config.kick);
                }));
                metrics.addCustomChart(new Metrics.SimplePie("algorithm", () -> {
                    if (!Config.sendUsage) {
                        return null;
                    }

                    return (Config.consensus >= 0.0d) ? "consensus" : "cascade";
                }));
                metrics.addCustomChart(new Metrics.SimplePie("messaging", () -> {
                    if (!Config.sendUsage) {
                        return null;
                    }

                    return (ServiceLocator.getService(IMessageHandler.class) instanceof RabbitMessageHandler) ? "RabbitMQ" : "BungeeCord";
                }));
                metrics.addCustomChart(new Metrics.SimplePie("redis", () -> {
                    if (!Config.sendUsage) {
                        return null;
                    }

                    return (ServiceLocator.getService(JedisPool.class) == null) ? "no" : "yes";
                }));
                metrics.addCustomChart(new Metrics.SimplePie("sql", () -> {
                    if (!Config.sendUsage) {
                        return null;
                    }

                    return (ServiceLocator.getService(ISQL.class).getType() == BaseSQLType.SQLite) ? "SQLite" : "MySQL";
                }));
            }
        });
        ThreadUtil.submit(checkUpdate);
        if (exceptionHandler.hasLimit()) {
            exceptionHandlerFuture = ThreadUtil.schedule(checkExceptionLimitReached, 2L * 60L * 1000L);
        }
        ThreadUtil.schedule(onFetchQueueThread, 10L * 1000L);
    }

    @SuppressWarnings("resource")
    public void onDisable() {
        super.onDisable();

        ThreadUtil.shutdown(1000L);

        List<ISQL> sqls = ServiceLocator.removeServices(ISQL.class);
        for (ISQL sql : sqls) {
            sql.disconnect();
        }

        JedisPool jedisPool = ServiceLocator.getService(JedisPool.class);
        if (jedisPool != null) {
            jedisPool.close();
        }

        List<IMessageHandler> services = ServiceLocator.removeServices(IMessageHandler.class);
        for (IMessageHandler handler : services) {
            try {
                handler.close();
            } catch (Exception ex) {

            }
        }

        ServiceLocator.getService(CommandProcessor.class).clear();
        ServiceLocator.getService(EventProcessor.class).clear();

        exceptionHandler.close();

        disableMessage();
    }

    // private
    private Runnable onFetchQueueThread = new Runnable() {
        public void run() {
            CountDownLatch latch = new CountDownLatch(1);

            BiConsumer<Object, CompleteEventArgs<?>> complete = (s, e) -> {
                latch.countDown();
                ISQL sql = ServiceLocator.getService(ISQL.class);
                if (sql.getType() == BaseSQLType.MySQL) {
                    FetchQueueMySQLCommand c = (FetchQueueMySQLCommand) s;
                    c.onComplete().detatchAll();
                } else if (sql.getType() == BaseSQLType.SQLite) {
                    ClearDataSQLiteCommand c = (ClearDataSQLiteCommand) s;
                    c.onComplete().detatchAll();
                }
            };

            ISQL sql = ServiceLocator.getService(ISQL.class);
            if (sql.getType() == BaseSQLType.MySQL) {
                FetchQueueMySQLCommand command = new FetchQueueMySQLCommand();
                command.onComplete().attach(complete);
                command.start();
            } else if (sql.getType() == BaseSQLType.SQLite) {
                ClearDataSQLiteCommand command = new ClearDataSQLiteCommand();
                command.onComplete().attach(complete);
                command.start();
            }

            try {
                latch.await();
            } catch (Exception ex) {
                IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
                if (handler != null) {
                    handler.sendException(ex);
                }
            }

            ThreadUtil.schedule(onFetchQueueThread, 10L * 1000L);
        }
    };
    private Runnable checkUpdate = new Runnable() {
        public void run() {
            if (isUpdateAvailable()) {
                return;
            }
            if (!Config.checkUpdates) {
                ThreadUtil.schedule(checkUpdate, 60L * 60L * 1000L);
                return;
            }

            boolean update = false;

            try {
                update = checkUpdate();
            } catch (Exception ex) {
                printWarning("Could not check for update.");
                ex.printStackTrace();
                ThreadUtil.schedule(checkUpdate, 60L * 60L * 1000L);
                return;
            }

            if (!update) {
                ThreadUtil.schedule(checkUpdate, 60L * 60L * 1000L);
                return;
            }

            String latestVersion = null;
            try {
                latestVersion = getLatestVersion();
            } catch (Exception ex) {
                ThreadUtil.schedule(checkUpdate, 60L * 60L * 1000L);
                return;
            }

            printInfo(ChatColor.AQUA + "Update available! New version: " + ChatColor.YELLOW + latestVersion);
            if (Config.notifyUpdates) {
                for (ProxiedPlayer player : getProxy().getPlayers()) {
                    if (player.hasPermission("avpn.admin")) {
                        player.sendMessage(new TextComponent(
                            ChatColor.AQUA + "Anti-VPN (Bungee) has an " + ChatColor.GREEN + "update" + ChatColor.AQUA + " available! New version: " + ChatColor.YELLOW + latestVersion));
                    }
                }
            }

            ThreadUtil.schedule(checkUpdate, 60L * 60L * 1000L);
        }
    };
    private Runnable checkExceptionLimitReached = new Runnable() {
        public void run() {
            if (!Config.sendErrors) {
                return;
            }

            if (exceptionHandler instanceof NullExceptionHandler) {
                swapExceptionHandlers(new RollbarExceptionHandler(Config.ROLLBAR_KEY, "production", version, getServerId(), getDescription().getName()));
            } else {
                if (exceptionHandler.isLimitReached()) {
                    swapExceptionHandlers(new GameAnalyticsExceptionHandler(Config.GAMEANALYTICS_KEY, Config.GAMEANALYTICS_SECRET, version, getServerId(), getDescription().getName()));
                }
            }

            if (exceptionHandler.hasLimit()) {
                exceptionHandlerFuture = ThreadUtil.schedule(checkExceptionLimitReached, 10L * 60L * 1000L);
            }
        }
    };

    public void swapExceptionHandlers(IExceptionHandler newHandler) {
        if (exceptionHandlerFuture != null) {
            exceptionHandlerFuture.cancel(false);
            if (!exceptionHandlerFuture.isDone() && !exceptionHandlerFuture.isCancelled()) {
                // Running
                try {
                    exceptionHandlerFuture.get();
                } catch (Exception ex) {

                }
            }
        }

        List<IExceptionHandler> oldHandlers = ServiceLocator.removeServices(IExceptionHandler.class);

        exceptionHandler = newHandler;
        ServiceLocator.provideService(exceptionHandler);

        Logger logger = getLogger();
        if (exceptionHandler instanceof Handler) {
            logger.addHandler((Handler) exceptionHandler);
        }

        for (IExceptionHandler handler : oldHandlers) {
            if (handler instanceof Handler) {
                logger.removeHandler((Handler) handler);
            }

            handler.close();
            exceptionHandler.addLogs(handler.getUnsentLogs());
        }

        if (exceptionHandler.hasLimit()) {
            exceptionHandlerFuture = ThreadUtil.schedule(checkExceptionLimitReached, 2L * 60L * 1000L);
        }
    }

    private void enableMessage() {
        printInfo(ChatColor.GREEN + "Enabled.");
        printInfo(ChatColor.AQUA + "[Version " + getDescription().getVersion() + "] " + ChatColor.DARK_GREEN + numCommands + " commands " + ChatColor.LIGHT_PURPLE + numEvents + " events "
            + ChatColor.BLUE + numMessages + " message handlers");
    }

    private void disableMessage() {
        printInfo(ChatColor.RED + "Disabled");
    }
}

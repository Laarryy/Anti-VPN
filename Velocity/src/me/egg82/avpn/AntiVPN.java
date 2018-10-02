package me.egg82.avpn;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiConsumer;
import java.util.logging.Handler;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

import me.egg82.avpn.core.RedisSubscriber;
import me.egg82.avpn.debug.IDebugPrinter;
import me.egg82.avpn.debug.VelocityDebugPrinter;
import me.egg82.avpn.sql.mysql.FetchQueueMySQLCommand;
import me.egg82.avpn.sql.sqlite.ClearDataSQLiteCommand;
import me.egg82.avpn.utils.RedisUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import ninja.egg82.analytics.exceptions.GameAnalyticsExceptionHandler;
import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.analytics.exceptions.NullExceptionHandler;
import ninja.egg82.analytics.exceptions.RollbarExceptionHandler;
import ninja.egg82.enums.BaseSQLType;
import ninja.egg82.events.CompleteEventArgs;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.enums.SenderType;
import ninja.egg82.plugin.messaging.IMessageHandler;
import ninja.egg82.plugin.utils.PluginReflectUtil;
import ninja.egg82.sql.ISQL;
import ninja.egg82.utils.ThreadUtil;
import ninja.egg82.utils.TimeUtil;
import ninja.egg82.velocity.BasePlugin;
import ninja.egg82.velocity.processors.CommandProcessor;
import ninja.egg82.velocity.processors.EventProcessor;
import redis.clients.jedis.Jedis;

@Plugin(id = "antivpn", name = "AntiVPN", version = "2.4.14", description = "Get the best; save money on overpriced plugins and block VPN users!", authors = { "egg82" })
public class AntiVPN extends BasePlugin {
    // vars
    private int numMessages = 0;
    private int numCommands = 0;
    private int numEvents = 0;

    private ScheduledFuture<?> exceptionHandlerFuture = null;
    private IExceptionHandler exceptionHandler = null;
    private String version = null;

    private static DecimalFormat format = new DecimalFormat(".##");

    // constructor
    @Inject
    public AntiVPN(ProxyServer proxy, Logger logger) {
        super(proxy, logger);
    }

    // public
    public static VPNAPI getAPI() {
        return VPNAPI.getInstance();
    }

    // private
    protected void onStartupComplete() {
        // BungeeCord onLoad
        version = getPluginVersion();

        exceptionHandler = ServiceLocator.getService(IExceptionHandler.class);
        ServiceLocator.provideService(VelocityDebugPrinter.class);

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

        // BungeeCord onEnable
        if (Config.sendErrors) {
            swapExceptionHandlers(new RollbarExceptionHandler(Config.ROLLBAR_KEY, "production", version, getServerId(), getPluginName()));
        }

        List<IMessageHandler> services = ServiceLocator.removeServices(IMessageHandler.class);
        for (IMessageHandler handler : services) {
            try {
                handler.close();
            } catch (Exception ex) {

            }
        }

        ThreadUtil.rename(getPluginName());

        Loaders.loadRedis();
        MessagingLoader.loadMessaging("avpn", null, getServerId(), SenderType.PROXY);
        Loaders.loadStorage(getPluginName(), getClass().getClassLoader(),
            new File(ServiceLocator.getService(BasePlugin.class).getDescription().getSource().orElse(null).getParent().toFile(), ServiceLocator.getService(BasePlugin.class).getPluginName()));

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

        if (exceptionHandler.hasLimit()) {
            exceptionHandlerFuture = ThreadUtil.schedule(checkExceptionLimitReached, 2L * 60L * 1000L);
        }
        ThreadUtil.schedule(onFetchQueueThread, 10L * 1000L);
    }

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
    private Runnable checkExceptionLimitReached = new Runnable() {
        public void run() {
            if (!Config.sendErrors) {
                return;
            }

            if (exceptionHandler instanceof NullExceptionHandler) {
                swapExceptionHandlers(new RollbarExceptionHandler(Config.ROLLBAR_KEY, "production", version, getServerId(), getPluginName()));
            } else {
                if (exceptionHandler.isLimitReached()) {
                    swapExceptionHandlers(new GameAnalyticsExceptionHandler(Config.GAMEANALYTICS_KEY, Config.GAMEANALYTICS_SECRET, version, getServerId(), getPluginName()));
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

        java.util.logging.Logger utilLogger = getJULLogger();
        if (exceptionHandler instanceof Handler) {
            utilLogger.addHandler((Handler) exceptionHandler);
        }

        for (IExceptionHandler handler : oldHandlers) {
            if (handler instanceof Handler) {
                utilLogger.removeHandler((Handler) handler);
            }

            handler.close();
            exceptionHandler.addLogs(handler.getUnsentLogs());
        }

        if (exceptionHandler.hasLimit()) {
            exceptionHandlerFuture = ThreadUtil.schedule(checkExceptionLimitReached, 2L * 60L * 1000L);
        }
    }

    private void enableMessage() {
        printInfo("Enabled.", TextColor.GREEN);
        printInfo(
            TextComponent.of("[Version " + getPluginVersion() + "] ", TextColor.AQUA).append(TextComponent.of(numCommands + " commands ", TextColor.DARK_GREEN))
                .append(TextComponent.of(numEvents + " events ", TextColor.LIGHT_PURPLE)).append(TextComponent.of(numMessages + " message handlers", TextColor.BLUE)));
    }
}

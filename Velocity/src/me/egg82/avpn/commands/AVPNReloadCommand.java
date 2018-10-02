package me.egg82.avpn.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.velocitypowered.api.command.CommandSource;

import me.egg82.avpn.AntiVPN;
import me.egg82.avpn.Config;
import me.egg82.avpn.Configuration;
import me.egg82.avpn.Loaders;
import me.egg82.avpn.MessagingLoader;
import me.egg82.avpn.core.RedisSubscriber;
import me.egg82.avpn.debug.IDebugPrinter;
import me.egg82.avpn.registries.IPRegistry;
import me.egg82.avpn.utils.RedisUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import ninja.egg82.analytics.exceptions.NullExceptionHandler;
import ninja.egg82.analytics.exceptions.RollbarExceptionHandler;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.enums.SenderType;
import ninja.egg82.plugin.handlers.async.AsyncCommandHandler;
import ninja.egg82.plugin.messaging.IMessageHandler;
import ninja.egg82.plugin.utils.DirectoryUtil;
import ninja.egg82.sql.ISQL;
import ninja.egg82.utils.ThreadUtil;
import ninja.egg82.utils.TimeUtil;
import ninja.egg82.velocity.BasePlugin;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class AVPNReloadCommand extends AsyncCommandHandler {
    // vars
    private static DecimalFormat format = new DecimalFormat(".##");

    // constructor
    public AVPNReloadCommand() {
        super();
    }

    // public

    // private
    @SuppressWarnings("resource")
    protected void onExecute(long elapsedMilliseconds) {
        if (!sender.hasPermission("avpn.admin")) {
            sender.sendMessage(TextComponent.of("You do not have permission to use this command!", TextColor.RED).content());
            return;
        }
        if (args.length != 0) {
            sender.sendMessage(TextComponent.of("Incorrect command usage!", TextColor.RED).content());
            String name = getClass().getSimpleName();
            name = name.substring(0, name.length() - 7).toLowerCase();
            ServiceLocator.getService(BasePlugin.class).getProxy().getCommandManager().execute((CommandSource) sender.getHandle(), "? " + name);
            return;
        }

        // Config
        File configFile = new File(
            new File(ServiceLocator.getService(BasePlugin.class).getDescription().getSource().orElse(null).getParent().toFile(), ServiceLocator.getService(BasePlugin.class).getPluginName()),
            "config.yml");
        if (configFile.exists() && configFile.isDirectory()) {
            DirectoryUtil.delete(configFile);
        }
        if (!configFile.exists()) {
            try (InputStreamReader reader = new InputStreamReader(ServiceLocator.getService(BasePlugin.class).getClass().getResourceAsStream("config.yml"));
                BufferedReader in = new BufferedReader(reader);
                FileWriter writer = new FileWriter(configFile);
                BufferedWriter out = new BufferedWriter(writer)) {
                while (in.ready()) {
                    writer.write(in.readLine());
                }
            } catch (Exception ex) {

            }
        }

        ConfigurationLoader<ConfigurationNode> loader = YAMLConfigurationLoader.builder().setIndent(2).setFile(configFile).build();
        ConfigurationNode root = null;
        try {
            root = loader.load();
        } catch (Exception ex) {
            throw new RuntimeException("Error loading config. Aborting plugin load.", ex);
        }
        Configuration config = new Configuration(root);
        ServiceLocator.removeServices(Configuration.class);
        ServiceLocator.provideService(config);

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
        boolean oldSendErrors = Config.sendErrors;
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

        if (oldSendErrors != Config.sendErrors) {
            if (Config.sendErrors) {
                ThreadUtil.submit(new Runnable() {
                    public void run() {
                        AntiVPN plugin = ServiceLocator.getService(AntiVPN.class);
                        plugin.swapExceptionHandlers(
                            new RollbarExceptionHandler(Config.ROLLBAR_KEY, "production", plugin.getPluginVersion(), plugin.getServerId(), plugin.getPluginName()));
                    }
                });
            } else {
                ThreadUtil.submit(new Runnable() {
                    public void run() {
                        ServiceLocator.getService(AntiVPN.class).swapExceptionHandlers(new NullExceptionHandler());
                    }
                });
            }
        }

        // Memory caches
        ServiceLocator.removeServices(IPRegistry.class);
        ServiceLocator.provideService(IPRegistry.class);

        // Redis
        JedisPool redisPool = ServiceLocator.getService(JedisPool.class);
        if (redisPool != null) {
            redisPool.close();
        }

        Loaders.loadRedis();

        ThreadUtil.scheduleInfinite(new Runnable() {
            public void run() {
                try (Jedis redis = RedisUtil.getRedis()) {
                    if (redis != null) {
                        redis.subscribe(new RedisSubscriber(), "avpn", "avpn-consensus");
                    }
                }
            }
        }, 5L * 1000L);

        // Rabbit
        List<IMessageHandler> services = ServiceLocator.removeServices(IMessageHandler.class);
        for (IMessageHandler handler : services) {
            try {
                handler.close();
            } catch (Exception ex) {

            }
        }

        MessagingLoader.loadMessaging("avpn", null, ServiceLocator.getService(BasePlugin.class).getServerId(), SenderType.PROXY);

        if (ServiceLocator.hasService(IMessageHandler.class)) {
            ServiceLocator.getService(IMessageHandler.class).addHandlersFromPackage("me.egg82.avpn.messages");
        }

        // SQL
        List<ISQL> sqls = ServiceLocator.removeServices(ISQL.class);
        for (ISQL sql : sqls) {
            sql.disconnect();
        }

        Loaders.loadStorage(ServiceLocator.getService(BasePlugin.class).getPluginName(), getClass().getClassLoader(),
            new File(ServiceLocator.getService(BasePlugin.class).getDescription().getSource().orElse(null).getParent().toFile(), ServiceLocator.getService(BasePlugin.class).getPluginName()));

        sender.sendMessage(TextComponent.of("Configuration reloaded!", TextColor.GREEN).content());
    }

    protected void onUndo() {

    }
}

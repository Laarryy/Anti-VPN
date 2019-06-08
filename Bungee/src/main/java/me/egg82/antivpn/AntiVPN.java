package me.egg82.antivpn;

import co.aikar.commands.BungeeCommandManager;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.RegisteredCommand;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import me.egg82.antivpn.commands.AntiVPNCommand;
import me.egg82.antivpn.core.SQLFetchResult;
import me.egg82.antivpn.enums.SQLType;
import me.egg82.antivpn.events.PostLoginCheckHandler;
import me.egg82.antivpn.events.PostLoginUpdateNotifyHandler;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.extended.RabbitMQReceiver;
import me.egg82.antivpn.extended.RedisSubscriber;
import me.egg82.antivpn.hooks.PlayerAnalyticsHook;
import me.egg82.antivpn.hooks.PluginHook;
import me.egg82.antivpn.services.AnalyticsHelper;
import me.egg82.antivpn.services.Redis;
import me.egg82.antivpn.sql.MySQL;
import me.egg82.antivpn.sql.SQLite;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.ConfigurationFileUtil;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.event.EventPriority;
import ninja.egg82.events.BungeeEventSubscriber;
import ninja.egg82.events.BungeeEvents;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import ninja.egg82.updater.BungeeUpdater;
import org.bstats.bungeecord.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AntiVPN {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ExecutorService workPool = null;

    private BungeeCommandManager commandManager;

    private List<BungeeEventSubscriber<?>> events = new ArrayList<>();

    private Metrics metrics = null;

    private final Plugin plugin;

    public AntiVPN(Plugin plugin) {
        this.plugin = plugin;
    }

    public void onLoad() {
        if (!plugin.getProxy().getName().equalsIgnoreCase("waterfall")) {
            plugin.getProxy().getLogger().log(Level.INFO, ChatColor.AQUA + "====================================");
            plugin.getProxy().getLogger().log(Level.INFO, ChatColor.YELLOW + "Anti-VPN runs better on Waterfall!");
            plugin.getProxy().getLogger().log(Level.INFO, ChatColor.YELLOW + "https://whypaper.emc.gs/");
            plugin.getProxy().getLogger().log(Level.INFO, ChatColor.AQUA + "====================================");
        }
    }

    public void onEnable() {
        commandManager = new BungeeCommandManager(plugin);
        commandManager.enableUnstableAPI("help");

        loadServices();
        loadSQL();
        loadCommands();
        loadEvents();
        loadHooks();
        loadMetrics();

        plugin.getProxy().getConsole().sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.GREEN + "Enabled"));

        plugin.getProxy().getConsole().sendMessage(new TextComponent(LogUtil.getHeading()
                + ChatColor.YELLOW + "[" + ChatColor.AQUA + "Version " + ChatColor.WHITE + plugin.getDescription().getVersion() + ChatColor.YELLOW +  "] "
                + ChatColor.YELLOW + "[" + ChatColor.WHITE + commandManager.getRegisteredRootCommands().size() + ChatColor.GOLD + " Commands" + ChatColor.YELLOW +  "] "
                + ChatColor.YELLOW + "[" + ChatColor.WHITE + events.size() + ChatColor.BLUE + " Events" + ChatColor.YELLOW +  "]"
        ));

        checkUpdate();
    }

    public void onDisable() {
        commandManager.unregisterCommands();

        for (BungeeEventSubscriber<?> event : events) {
            event.cancel();
        }
        events.clear();

        unloadHooks();
        unloadServices();

        plugin.getProxy().getConsole().sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.DARK_RED + "Disabled"));
    }

    private void loadServices() {
        ConfigurationFileUtil.reloadConfig(plugin);

        loadServicesExternal();
        ServiceLocator.register(new BungeeUpdater(plugin, 58716));
    }

    public void loadServicesExternal() {
        workPool = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat("AntiVPN-%d").build());

        Optional<Configuration> config = ConfigUtil.getConfig();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!config.isPresent() || !cachedConfig.isPresent()) {
            return;
        }

        workPool.submit(() -> new RedisSubscriber(cachedConfig.get().getRedisPool(), config.get().getNode("redis")));
        ServiceLocator.register(new RabbitMQReceiver(cachedConfig.get().getRabbitConnectionFactory()));
    }

    private void loadSQL() {
        Optional<Configuration> config = ConfigUtil.getConfig();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!config.isPresent() || !cachedConfig.isPresent()) {
            return;
        }

        if (cachedConfig.get().getSQLType() == SQLType.MySQL) {
            MySQL.createTables(cachedConfig.get().getSQL(), config.get().getNode("storage")).thenRun(() ->
                    MySQL.loadInfo(cachedConfig.get().getSQL(), config.get().getNode("storage")).thenAccept(v -> {
                        Redis.updateFromQueue(v, cachedConfig.get().getSourceCacheTime(), cachedConfig.get().getRedisPool(), config.get().getNode("redis"));
                        updateSQL();
                    })
            );
        } else if (cachedConfig.get().getSQLType() == SQLType.SQLite) {
            SQLite.createTables(cachedConfig.get().getSQL(), config.get().getNode("storage")).thenRun(() ->
                    SQLite.loadInfo(cachedConfig.get().getSQL(), config.get().getNode("storage")).thenAccept(v -> {
                        Redis.updateFromQueue(v, cachedConfig.get().getSourceCacheTime(), cachedConfig.get().getRedisPool(), config.get().getNode("redis"));
                        updateSQL();
                    })
            );
        }
    }

    public void loadSQLExternal() {
        Optional<Configuration> config = ConfigUtil.getConfig();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!config.isPresent() || !cachedConfig.isPresent()) {
            return;
        }

        if (cachedConfig.get().getSQLType() == SQLType.MySQL) {
            MySQL.createTables(cachedConfig.get().getSQL(), config.get().getNode("storage")).thenRun(() ->
                    MySQL.loadInfo(cachedConfig.get().getSQL(), config.get().getNode("storage")).thenAccept(v -> {
                        Redis.updateFromQueue(v, cachedConfig.get().getSourceCacheTime(), cachedConfig.get().getRedisPool(), config.get().getNode("redis"));
                    })
            );
        } else if (cachedConfig.get().getSQLType() == SQLType.SQLite) {
            SQLite.createTables(cachedConfig.get().getSQL(), config.get().getNode("storage")).thenRun(() ->
                    SQLite.loadInfo(cachedConfig.get().getSQL(), config.get().getNode("storage")).thenAccept(v -> {
                        Redis.updateFromQueue(v, cachedConfig.get().getSourceCacheTime(), cachedConfig.get().getRedisPool(), config.get().getNode("redis"));
                    })
            );
        }
    }

    private void updateSQL() {
        workPool.submit(() -> {
            try {
                Thread.sleep(10000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            Optional<Configuration> config = ConfigUtil.getConfig();
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!config.isPresent() || !cachedConfig.isPresent()) {
                return;
            }

            SQLFetchResult result = null;

            try {
                if (cachedConfig.get().getSQLType() == SQLType.MySQL) {
                    result = MySQL.fetchQueue(cachedConfig.get().getSQL(), config.get().getNode("storage"), cachedConfig.get().getSourceCacheTime()).get();
                }

                if (result != null) {
                    Redis.updateFromQueue(result, cachedConfig.get().getSourceCacheTime(), cachedConfig.get().getRedisPool(), config.get().getNode("redis")).get();
                }
            } catch (ExecutionException ex) {
                logger.error(ex.getMessage(), ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            updateSQL();
        });
    }

    private void loadCommands() {
        commandManager.getCommandConditions().addCondition(String.class, "ip", (c, exec, value) -> {
            if (!ValidationUtil.isValidIp(value)) {
                throw new ConditionFailedException("Value must be a valid IP address.");
            }
        });
        commandManager.getCommandConditions().addCondition(String.class, "source", (c, exec, value) -> {
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!cachedConfig.isPresent()) {
                return;
            }

            if (!cachedConfig.get().getSources().contains(value)) {
                throw new ConditionFailedException("Value must be a valid source name.");
            }
        });

        commandManager.getCommandCompletions().registerCompletion("subcommand", c -> {
            String lower = c.getInput().toLowerCase();
            Set<String> commands = new LinkedHashSet<>();
            SetMultimap<String, RegisteredCommand> subcommands = commandManager.getRootCommand("antivpn").getSubCommands();
            for (Map.Entry<String, RegisteredCommand> kvp : subcommands.entries()) {
                if (!kvp.getValue().isPrivate() && (lower.isEmpty() || kvp.getKey().toLowerCase().startsWith(lower)) && kvp.getValue().getCommand().indexOf(' ') == -1) {
                    commands.add(kvp.getValue().getCommand());
                }
            }
            return ImmutableList.copyOf(commands);
        });

        commandManager.registerCommand(new AntiVPNCommand(this, plugin));
    }

    private void loadEvents() {
        events.add(BungeeEvents.subscribe(plugin, PostLoginEvent.class, EventPriority.LOWEST).handler(e -> new PostLoginCheckHandler().accept(e)));
        events.add(BungeeEvents.subscribe(plugin, PostLoginEvent.class, EventPriority.LOW).handler(e -> new PostLoginUpdateNotifyHandler().accept(e)));
    }

    private void loadHooks() {
        PluginManager manager = plugin.getProxy().getPluginManager();

        if (manager.getPlugin("Plan") != null) {
            plugin.getProxy().getConsole().sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.GREEN + "Enabling support for Plan."));
            ServiceLocator.register(new PlayerAnalyticsHook(plugin));
        } else {
            plugin.getProxy().getConsole().sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.YELLOW + "Plan was not found. Personal analytics support has been disabled."));
        }
    }

    private void loadMetrics() {
        metrics = new Metrics(plugin);
        metrics.addCustomChart(new Metrics.SimplePie("sql", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            if (!config.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            return config.get().getNode("storage", "method").getString("sqlite");
        }));
        metrics.addCustomChart(new Metrics.SimplePie("redis", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            if (!config.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            return config.get().getNode("redis", "enabled").getBoolean(false) ? "yes" : "no";
        }));
        metrics.addCustomChart(new Metrics.SimplePie("rabbitmq", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            if (!config.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            return config.get().getNode("rabbitmq", "enabled").getBoolean(false) ? "yes" : "no";
        }));
        metrics.addCustomChart(new Metrics.AdvancedPie("sources", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!config.isPresent() || !cachedConfig.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            Map<String, Integer> values = new HashMap<>();
            for (String key : cachedConfig.get().getSources()) {
                values.put(key, 1);
            }
            return values;
        }));
        metrics.addCustomChart(new Metrics.SimplePie("kick", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            if (!config.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            return config.get().getNode("kick", "enabled").getBoolean(true) ? "yes" : "no";
        }));
        metrics.addCustomChart(new Metrics.SimplePie("algorithm", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            if (!config.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            return config.get().getNode("kick", "method").getString("cascade");
        }));
        metrics.addCustomChart(new Metrics.SingleLineChart("blocked", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            if (!config.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            return (int) AnalyticsHelper.getBlocked();
        }));
    }

    private void checkUpdate() {
        Optional<Configuration> config = ConfigUtil.getConfig();
        if (!config.isPresent()) {
            return;
        }

        BungeeUpdater updater;
        try {
            updater = ServiceLocator.get(BungeeUpdater.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (!config.get().getNode("update", "check").getBoolean(true)) {
            return;
        }

        updater.isUpdateAvailable().thenAccept(v -> {
            if (!v) {
                return;
            }

            if (config.get().getNode("update", "notify").getBoolean(true)) {
                try {
                    plugin.getProxy().getConsole().sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.AQUA + " has an " + ChatColor.GREEN + "update" + ChatColor.AQUA + " available! New version: " + ChatColor.YELLOW + updater.getLatestVersion().get()));
                } catch (ExecutionException ex) {
                    logger.error(ex.getMessage(), ex);
                } catch (InterruptedException ex) {
                    logger.error(ex.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void unloadHooks() {
        Optional<? extends PluginHook> plan;
        try {
            plan = ServiceLocator.getOptional(PlayerAnalyticsHook.class);
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
            plan = Optional.empty();
        }
        plan.ifPresent(v -> v.cancel());
    }

    public void unloadServices() {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            return;
        }

        RabbitMQReceiver rabbitReceiver;

        try {
            rabbitReceiver = ServiceLocator.get(RabbitMQReceiver.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (cachedConfig.get().getRedisPool() != null) {
            cachedConfig.get().getRedisPool().close();
        }

        try {
            rabbitReceiver.close();
        } catch (IOException | TimeoutException ignored) {}

        if (!workPool.isShutdown()) {
            workPool.shutdown();
            try {
                if (!workPool.awaitTermination(8L, TimeUnit.SECONDS)) {
                    workPool.shutdownNow();
                }
            } catch (InterruptedException ignored) {
                workPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        cachedConfig.get().getSQL().close();
    }
}

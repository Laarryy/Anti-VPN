package me.egg82.antivpn;

import co.aikar.commands.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import me.egg82.antivpn.apis.SourceAPI;
import me.egg82.antivpn.commands.AntiVPNCommand;
import me.egg82.antivpn.enums.Message;
import me.egg82.antivpn.events.EventHolder;
import me.egg82.antivpn.events.PlayerEvents;
import me.egg82.antivpn.events.PostLoginUpdateNotifyHandler;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.hooks.PlayerAnalyticsHook;
import me.egg82.antivpn.hooks.PluginHook;
import me.egg82.antivpn.messaging.RabbitMQ;
import me.egg82.antivpn.services.AnalyticsHelper;
import me.egg82.antivpn.services.GameAnalyticsErrorHandler;
import me.egg82.antivpn.services.PluginMessageFormatter;
import me.egg82.antivpn.services.StorageMessagingHandler;
import me.egg82.antivpn.storage.MySQL;
import me.egg82.antivpn.storage.SQLite;
import me.egg82.antivpn.storage.Storage;
import me.egg82.antivpn.utils.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.ScheduledTask;
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

    private ExecutorService workPool = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("AntiVPN-%d").build());

    private BungeeCommandManager commandManager;

    private List<EventHolder> eventHolders = new ArrayList<>();
    private List<BungeeEventSubscriber<?>> events = new ArrayList<>();
    private List<ScheduledTask> tasks = new ArrayList<>();

    private Metrics metrics = null;

    private final Plugin plugin;

    private CommandIssuer consoleCommandIssuer = null;

    public AntiVPN(Plugin plugin) { this.plugin = plugin; }

    public void onLoad() {
        if (BungeeEnvironmentUtil.getEnvironment() != BungeeEnvironmentUtil.Environment.WATERFALL) {
            plugin.getProxy().getLogger().log(Level.INFO, ChatColor.AQUA + "====================================");
            plugin.getProxy().getLogger().log(Level.INFO, ChatColor.YELLOW + "Anti-VPN runs better on Waterfall!");
            plugin.getProxy().getLogger().log(Level.INFO, ChatColor.YELLOW + "https://whypaper.emc.gs/");
            plugin.getProxy().getLogger().log(Level.INFO, ChatColor.AQUA + "====================================");
        }
    }

    public void onEnable() {
        GameAnalyticsErrorHandler.open(ServerIDUtil.getID(new File(plugin.getDataFolder(), "stats-id.txt")), plugin.getDescription().getVersion(), plugin.getProxy().getVersion());

        commandManager = new BungeeCommandManager(plugin);
        commandManager.enableUnstableAPI("help");

        consoleCommandIssuer = commandManager.getCommandIssuer(plugin.getProxy().getConsole());

        loadServices();
        loadLanguages();
        loadCommands();
        loadEvents();
        loadTasks();
        loadHooks();
        loadMetrics();

        int numEvents = events.size();
        for (EventHolder eventHolder : eventHolders) {
            numEvents += eventHolder.numEvents();
        }

        consoleCommandIssuer.sendInfo(Message.GENERAL__ENABLED);
        consoleCommandIssuer.sendInfo(Message.GENERAL__LOAD,
                "{version}", plugin.getDescription().getVersion(),
                "{commands}", String.valueOf(commandManager.getRegisteredRootCommands().size()),
                "{events}", String.valueOf(numEvents),
                "{tasks}", String.valueOf(tasks.size())
        );

        workPool.execute(this::checkUpdate);
    }

    public void onDisable() {
        workPool.shutdown();
        try {
            if (!workPool.awaitTermination(4L, TimeUnit.SECONDS)) {
                workPool.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        commandManager.unregisterCommands();

        for (ScheduledTask task : tasks) {
            plugin.getProxy().getScheduler().cancel(task);
        }
        tasks.clear();

        for (EventHolder eventHolder : eventHolders) {
            eventHolder.cancel();
        }
        eventHolders.clear();
        for (BungeeEventSubscriber<?> event : events) {
            event.cancel();
        }
        events.clear();

        unloadHooks();
        unloadServices();

        consoleCommandIssuer.sendInfo(Message.GENERAL__DISABLED);

        GameAnalyticsErrorHandler.close();
    }

    private void loadLanguages() {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new RuntimeException("Cached config could not be fetched.");
        }

        BungeeLocales locales = commandManager.getLocales();

        try {
            for (Locale locale : Locale.getAvailableLocales()) {
                Optional<File> localeFile = LanguageFileUtil.getLanguage(plugin, locale);
                if (localeFile.isPresent()) {
                    commandManager.addSupportedLanguage(locale);
                    locales.loadYamlLanguageFile(localeFile.get(), locale);
                }
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }

        locales.loadLanguages();
        locales.setDefaultLocale(cachedConfig.get().getLanguage());
        commandManager.usePerIssuerLocale(true);

        commandManager.setFormat(MessageType.ERROR, new PluginMessageFormatter(commandManager, Message.GENERAL__HEADER));
        commandManager.setFormat(MessageType.INFO, new PluginMessageFormatter(commandManager, Message.GENERAL__HEADER));
        commandManager.setFormat(MessageType.ERROR, ChatColor.DARK_RED, ChatColor.YELLOW, ChatColor.AQUA, ChatColor.WHITE);
        commandManager.setFormat(MessageType.INFO, ChatColor.WHITE, ChatColor.YELLOW, ChatColor.AQUA, ChatColor.GREEN, ChatColor.RED, ChatColor.GOLD, ChatColor.BLUE, ChatColor.GRAY);
    }

    private void loadServices() {
        StorageMessagingHandler handler = new StorageMessagingHandler();
        ServiceLocator.register(handler);
        ConfigurationFileUtil.reloadConfig(plugin, handler, handler);

        ServiceLocator.register(new BungeeUpdater(plugin, 58716));
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
            for (Map.Entry<String, SourceAPI> kvp : cachedConfig.get().getSources().entrySet()) {
                if (kvp.getKey().equalsIgnoreCase(value)) {
                    return;
                }
            }
            throw new ConditionFailedException("Value must be a valid source name.");
        });

        commandManager.getCommandConditions().addCondition(String.class, "storage", (c, exec, value) -> {
            String v = value.replace(" ", "_");
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!cachedConfig.isPresent()) {
                return;
            }
            for (Storage s : cachedConfig.get().getStorage()) {
                if (s.getClass().getSimpleName().equalsIgnoreCase(v)) {
                    return;
                }
            }
            throw new ConditionFailedException("Value must be a valid storage name.");
        });

        commandManager.getCommandCompletions().registerCompletion("storage", c -> {
            String lower = c.getInput().toLowerCase().replace(" ", "_");
            Set<String> storage = new LinkedHashSet<>();
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!cachedConfig.isPresent()) {
                logger.error("Cached config could not be fetched.");
                return ImmutableList.copyOf(storage);
            }
            for (Storage s : cachedConfig.get().getStorage()) {
                String ss = s.getClass().getSimpleName();
                if (ss.toLowerCase().startsWith(lower)) {
                    storage.add(ss);
                }
            }
            return ImmutableList.copyOf(storage);
        });

        commandManager.getCommandCompletions().registerCompletion("player", c -> {
            String lower = c.getInput().toLowerCase();
            Set<String> players = new LinkedHashSet<>();
            for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                if (lower.isEmpty() || p.getName().toLowerCase().startsWith(lower)) {
                    players.add(p.getName());
                }
            }
            return ImmutableList.copyOf(players);
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

        commandManager.registerCommand(new AntiVPNCommand(plugin));
    }

    private void loadEvents() {
        events.add(BungeeEvents.subscribe(plugin, PostLoginEvent.class, EventPriority.LOW).handler(e -> new PostLoginUpdateNotifyHandler(plugin, commandManager).accept(e)));
        eventHolders.add(new PlayerEvents(plugin));
    }

    private void loadTasks() { }

    private void loadHooks() {
        PluginManager manager = plugin.getProxy().getPluginManager();

        if (manager.getPlugin("Plan") != null) {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_ENABLE, "{plugin}", "Plan");
            ServiceLocator.register(new PlayerAnalyticsHook());
        } else {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_DISABLE, "{plugin}", "Plan");
        }
    }

    private void loadMetrics() {
        metrics = new Metrics(plugin);
        metrics.addCustomChart(new Metrics.SingleLineChart("blocked_vpns", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            if (!config.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            return (int) AnalyticsHelper.getBlockedVPNs();
        }));
        metrics.addCustomChart(new Metrics.SingleLineChart("blocked_mcleaks", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            if (!config.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            return (int) AnalyticsHelper.getBlockedMCLeaks();
        }));
        metrics.addCustomChart(new Metrics.SimplePie("mysql", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!config.isPresent() || !cachedConfig.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            for (Storage s : cachedConfig.get().getStorage()) {
                if (s instanceof MySQL) {
                    return "yes";
                }
            }

            return "no";
        }));
        metrics.addCustomChart(new Metrics.SimplePie("redis_storage", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!config.isPresent() || !cachedConfig.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            for (Storage s : cachedConfig.get().getStorage()) {
                if (s instanceof me.egg82.antivpn.storage.Redis) {
                    return "yes";
                }
            }

            return "no";
        }));
        metrics.addCustomChart(new Metrics.SimplePie("sqlite", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!config.isPresent() || !cachedConfig.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            for (Storage s : cachedConfig.get().getStorage()) {
                if (s instanceof SQLite) {
                    return "yes";
                }
            }

            return "no";
        }));
        metrics.addCustomChart(new Metrics.SimplePie("redis_messaging", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!config.isPresent() || !cachedConfig.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            for (Storage s : cachedConfig.get().getStorage()) {
                if (s instanceof me.egg82.antivpn.messaging.Redis) {
                    return "yes";
                }
            }

            return "no";
        }));
        metrics.addCustomChart(new Metrics.SimplePie("rabbitmq", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!config.isPresent() || !cachedConfig.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            for (Storage s : cachedConfig.get().getStorage()) {
                if (s instanceof RabbitMQ) {
                    return "yes";
                }
            }

            return "no";
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
            for (Map.Entry<String, SourceAPI> kvp : cachedConfig.get().getSources().entrySet()) {
                values.put(kvp.getKey(), 1);
            }
            return values;
        }));
        metrics.addCustomChart(new Metrics.SimplePie("algorithm", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!config.isPresent() || !cachedConfig.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            return cachedConfig.get().getVPNAlgorithmMethod().getName();
        }));
        metrics.addCustomChart(new Metrics.SimplePie("vpn_action", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!config.isPresent() || !cachedConfig.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            if (!cachedConfig.get().getVPNKickMessage().isEmpty() && !cachedConfig.get().getVPNActionCommands().isEmpty()) {
                return "multi";
            } else if (!cachedConfig.get().getVPNKickMessage().isEmpty()) {
                return "kick";
            } else if (!cachedConfig.get().getVPNActionCommands().isEmpty()) {
                return "commands";
            }

            return "none";
        }));
        metrics.addCustomChart(new Metrics.SimplePie("mcleaks_action", () -> {
            Optional<Configuration> config = ConfigUtil.getConfig();
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!config.isPresent() || !cachedConfig.isPresent()) {
                return null;
            }

            if (!config.get().getNode("stats", "usage").getBoolean(true)) {
                return null;
            }

            if (!cachedConfig.get().getMCLeaksKickMessage().isEmpty() && !cachedConfig.get().getMCLeaksActionCommands().isEmpty()) {
                return "multi";
            } else if (!cachedConfig.get().getMCLeaksKickMessage().isEmpty()) {
                return "kick";
            } else if (!cachedConfig.get().getMCLeaksActionCommands().isEmpty()) {
                return "commands";
            }

            return "none";
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

            try {
                consoleCommandIssuer.sendInfo(Message.GENERAL__UPDATE, "{version}", updater.getLatestVersion().get());
            } catch (ExecutionException ex) {
                logger.error(ex.getMessage(), ex);
            } catch (InterruptedException ex) {
                logger.error(ex.getMessage(), ex);
                Thread.currentThread().interrupt();
            }
        });

        try {
            Thread.sleep(60L * 60L * 1000L);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        try {
            workPool.execute(this::checkUpdate);
        } catch (RejectedExecutionException ignored) { }
    }

    private void unloadHooks() {
        Set<? extends PluginHook> hooks = ServiceLocator.remove(PluginHook.class);
        for (PluginHook hook : hooks) {
            hook.cancel();
        }
    }

    public void unloadServices() {
        Optional<StorageMessagingHandler> storageMessagingHandler;
        try {
            storageMessagingHandler = ServiceLocator.getOptional(StorageMessagingHandler.class);
        } catch (IllegalAccessException | InstantiationException ex) {
            storageMessagingHandler = Optional.empty();
        }
        storageMessagingHandler.ifPresent(StorageMessagingHandler::close);
    }
}

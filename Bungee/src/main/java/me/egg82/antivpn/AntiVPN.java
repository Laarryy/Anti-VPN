package me.egg82.antivpn;

import co.aikar.commands.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import me.egg82.antivpn.api.*;
import me.egg82.antivpn.api.event.api.GenericAPIDisableEvent;
import me.egg82.antivpn.api.event.api.GenericAPILoadedEvent;
import me.egg82.antivpn.api.event.api.GenericPublicationErrorHandler;
import me.egg82.antivpn.api.model.ip.BungeeIPManager;
import me.egg82.antivpn.api.model.player.BungeePlayerManager;
import me.egg82.antivpn.api.model.source.GenericSourceManager;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.api.platform.BungeePlatform;
import me.egg82.antivpn.api.platform.BungeePluginMetadata;
import me.egg82.antivpn.api.platform.Platform;
import me.egg82.antivpn.api.platform.PluginMetadata;
import me.egg82.antivpn.bungee.BungeeEnvironmentUtil;
import me.egg82.antivpn.commands.AntiVPNCommand;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.config.ConfigurationFileUtil;
import me.egg82.antivpn.events.EventHolder;
import me.egg82.antivpn.events.PlayerEvents;
import me.egg82.antivpn.events.PostLoginUpdateNotifyHandler;
import me.egg82.antivpn.hooks.LuckPermsHook;
import me.egg82.antivpn.hooks.PlayerAnalyticsHook;
import me.egg82.antivpn.hooks.PluginHook;
import me.egg82.antivpn.lang.LanguageFileUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.lang.PluginMessageFormatter;
import me.egg82.antivpn.messaging.GenericMessagingHandler;
import me.egg82.antivpn.messaging.MessagingHandler;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.messaging.ServerIDUtil;
import me.egg82.antivpn.services.GameAnalyticsErrorHandler;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.utils.ValidationUtil;
import net.engio.mbassy.bus.MBassador;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;

public class AntiVPN {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ExecutorService workPool = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("AntiVPN-%d").build());

    private BungeeCommandManager commandManager;

    private final List<EventHolder> eventHolders = new ArrayList<>();
    private final List<BungeeEventSubscriber<?>> events = new ArrayList<>();
    private final IntList tasks = new IntArrayList();

    private final Plugin plugin;

    private CommandIssuer consoleCommandIssuer = null;

    public AntiVPN(@NonNull Plugin plugin) { this.plugin = plugin; }

    public void onLoad() {
        if (BungeeEnvironmentUtil.getEnvironment() != BungeeEnvironmentUtil.Environment.WATERFALL) {
            plugin.getProxy().getLogger().log(Level.INFO, ChatColor.AQUA + "====================================");
            plugin.getProxy().getLogger().log(Level.INFO, ChatColor.YELLOW + "Anti-VPN runs better on Waterfall!");
            plugin.getProxy().getLogger().log(Level.INFO, ChatColor.YELLOW + "https://whypaper.emc.gs/");
            plugin.getProxy().getLogger().log(Level.INFO, ChatColor.AQUA + "====================================");
        }
    }

    public void onEnable() {
        GameAnalyticsErrorHandler.open(ServerIDUtil.getId(new File(plugin.getDataFolder(), "stats-id.txt")), plugin.getDescription().getVersion(), ProxyServer.getInstance().getVersion());

        commandManager = new BungeeCommandManager(plugin);
        commandManager.enableUnstableAPI("help");

        setChatColors();

        consoleCommandIssuer = commandManager.getCommandIssuer(ProxyServer.getInstance().getConsole());

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
                "{apiversion}", VPNAPIProvider.getInstance().getPluginMetadata().getApiVersion(),
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

        for (int task : tasks) {
            ProxyServer.getInstance().getScheduler().cancel(task);
        }
        tasks.clear();

        VPNAPIProvider.getInstance().runUpdateTask().join();

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
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            throw new RuntimeException("CachedConfig seems to be null.");
        }

        BungeeLocales locales = commandManager.getLocales();

        try {
            for (Locale locale : Locale.getAvailableLocales()) {
                Optional<File> localeFile = LanguageFileUtil.getLanguage(plugin.getDataFolder(), locale);
                if (localeFile.isPresent()) {
                    commandManager.addSupportedLanguage(locale);
                    locales.loadYamlLanguageFile(localeFile.get(), locale);
                }
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }

        locales.loadLanguages();
        locales.setDefaultLocale(cachedConfig.getLanguage());
        commandManager.usePerIssuerLocale(true);

        commandManager.setFormat(MessageType.ERROR, new PluginMessageFormatter(commandManager, Message.GENERAL__HEADER));
        commandManager.setFormat(MessageType.INFO, new PluginMessageFormatter(commandManager, Message.GENERAL__HEADER));
        setChatColors();
    }

    private void setChatColors() {
        commandManager.setFormat(MessageType.ERROR, ChatColor.DARK_RED, ChatColor.YELLOW, ChatColor.AQUA, ChatColor.WHITE);
        commandManager.setFormat(MessageType.INFO, ChatColor.WHITE, ChatColor.YELLOW, ChatColor.AQUA, ChatColor.GREEN, ChatColor.RED, ChatColor.GOLD, ChatColor.BLUE, ChatColor.GRAY, ChatColor.DARK_RED);
    }

    private void loadServices() {
        GenericSourceManager sourceManager = new GenericSourceManager();

        MessagingHandler messagingHandler = new GenericMessagingHandler();
        ServiceLocator.register(messagingHandler);

        ConfigurationFileUtil.reloadConfig(plugin.getDataFolder(), consoleCommandIssuer, messagingHandler, sourceManager);

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        BungeeIPManager ipManager = new BungeeIPManager(sourceManager, cachedConfig.getCacheTime().getTime(), cachedConfig.getCacheTime().getUnit());
        BungeePlayerManager playerManager = new BungeePlayerManager(cachedConfig.getThreads(), cachedConfig.getMcLeaksKey(), cachedConfig.getCacheTime().getTime(), cachedConfig.getCacheTime().getUnit());
        Platform platform = new BungeePlatform(System.currentTimeMillis());
        PluginMetadata metadata = new BungeePluginMetadata(plugin.getDescription().getVersion());
        VPNAPI api = new GenericVPNAPI(platform, metadata, ipManager, playerManager, sourceManager, cachedConfig, new MBassador<>(new GenericPublicationErrorHandler()));

        APIUtil.setManagers(ipManager, playerManager, sourceManager);

        ServiceLocator.register(new BungeeUpdater(plugin, 58716));

        APIRegistrationUtil.register(api);

        api.getEventBus().post(new GenericAPILoadedEvent(api)).now();
    }

    private void loadCommands() {
        commandManager.getCommandConditions().addCondition(String.class, "ip", (c, exec, value) -> {
            if (!ValidationUtil.isValidIp(value)) {
                throw new ConditionFailedException("Value must be a valid IP address.");
            }
        });

        commandManager.getCommandConditions().addCondition(String.class, "source", (c, exec, value) -> {
            List<Source<? extends SourceModel>> sources = VPNAPIProvider.getInstance().getSourceManager().getSources();
            for (Source<? extends SourceModel> source : sources) {
                if (source.getName().equalsIgnoreCase(value)) {
                    return;
                }
            }

            throw new ConditionFailedException("Value must be a valid source name.");
        });

        commandManager.getCommandCompletions().registerCompletion("source", c -> {
            String lower = c.getInput().toLowerCase().replace(" ", "_");
            List<Source<? extends SourceModel>> sources = VPNAPIProvider.getInstance().getSourceManager().getSources();
            Set<String> retVal = new LinkedHashSet<>();

            for (Source<? extends SourceModel> source : sources) {
                String ss = source.getName();
                if (ss.toLowerCase().startsWith(lower)) {
                    retVal.add(ss);
                }
            }
            return ImmutableList.copyOf(retVal);
        });

        commandManager.getCommandConditions().addCondition(String.class, "storage", (c, exec, value) -> {
            String v = value.replace(" ", "_");
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (cachedConfig == null) {
                logger.error("Cached config could not be fetched.");
                return;
            }
            for (StorageService service : cachedConfig.getStorage()) {
                if (service.getName().equalsIgnoreCase(v)) {
                    return;
                }
            }
            throw new ConditionFailedException("Value must be a valid storage name.");
        });

        commandManager.getCommandCompletions().registerCompletion("storage", c -> {
            String lower = c.getInput().toLowerCase().replace(" ", "_");
            Set<String> retVal = new LinkedHashSet<>();
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (cachedConfig == null) {
                logger.error("Cached config could not be fetched.");
                return ImmutableList.copyOf(retVal);
            }
            for (StorageService service : cachedConfig.getStorage()) {
                String ss = service.getName();
                if (ss.toLowerCase().startsWith(lower)) {
                    retVal.add(ss);
                }
            }
            return ImmutableList.copyOf(retVal);
        });

        commandManager.getCommandCompletions().registerCompletion("player", c -> {
            String lower = c.getInput().toLowerCase();
            Set<String> players = new LinkedHashSet<>();
            for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
                if (lower.isEmpty() || p.getName().toLowerCase().startsWith(lower)) {
                    players.add(p.getName());
                }
            }
            return ImmutableList.copyOf(players);
        });

        commandManager.getCommandCompletions().registerCompletion("type", c -> {
            String lower = c.getInput().toLowerCase();
            Set<String> retVal = new LinkedHashSet<>();
            if ("vpn".startsWith(lower)) {
                retVal.add("vpn");
            }
            if ("mcleaks".startsWith(lower)) {
                retVal.add("mcleaks");
            }
            return ImmutableList.copyOf(retVal);
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

        commandManager.registerCommand(new AntiVPNCommand(plugin, consoleCommandIssuer));
    }

    private void loadEvents() {
        events.add(BungeeEvents.subscribe(plugin, PostLoginEvent.class, EventPriority.LOW).handler(e -> new PostLoginUpdateNotifyHandler(plugin, commandManager).accept(e)));
        eventHolders.add(new PlayerEvents(plugin, consoleCommandIssuer));
    }

    private void loadTasks() {
        tasks.add(ProxyServer.getInstance().getScheduler().schedule(plugin, () -> VPNAPIProvider.getInstance().runUpdateTask().join(), 1L, 1L, TimeUnit.SECONDS).getId());
    }

    private void loadHooks() {
        PluginManager manager = plugin.getProxy().getPluginManager();

        if (manager.getPlugin("Plan") != null) {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_ENABLE, "{plugin}", "Plan");
            ServiceLocator.register(new PlayerAnalyticsHook());
        } else {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_DISABLE, "{plugin}", "Plan");
        }

        if (manager.getPlugin("LuckPerms") != null) {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_ENABLE, "{plugin}", "LuckPerms");
            if (ConfigUtil.getDebugOrFalse()) {
                consoleCommandIssuer.sendMessage("<c2>Running actions on pre-login.</c2>");
            }
            ServiceLocator.register(new LuckPermsHook(consoleCommandIssuer));
        } else {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_DISABLE, "{plugin}", "LuckPerms");
            if (ConfigUtil.getDebugOrFalse()) {
                consoleCommandIssuer.sendMessage("<c2>Running actions on post-login.</c2>");
            }
        }
    }

    private static final AtomicLong blockedVPNs = new AtomicLong(0L);

    private static final AtomicLong blockedMCLeaks = new AtomicLong(0L);

    public static void incrementBlockedVPNs() { blockedVPNs.getAndIncrement(); }

    public static void incrementBlockedMCLeaks() { blockedMCLeaks.getAndIncrement(); }

    private void loadMetrics() {
        Metrics metrics = new Metrics(plugin, 3249);
        metrics.addCustomChart(new Metrics.SingleLineChart("blocked_vpns", () -> {
            ConfigurationNode config = ConfigUtil.getConfig();
            if (config == null) {
                return null;
            }

            if (!config.node("stats", "usage").getBoolean(true)) {
                return null;
            }

            return (int) blockedVPNs.getAndSet(0L);
        }));
        metrics.addCustomChart(new Metrics.SingleLineChart("blocked_mcleaks", () -> {
            ConfigurationNode config = ConfigUtil.getConfig();
            if (config == null) {
                return null;
            }

            if (!config.node("stats", "usage").getBoolean(true)) {
                return null;
            }

            return (int) blockedMCLeaks.getAndSet(0L);
        }));
        metrics.addCustomChart(new Metrics.AdvancedPie("storage", () -> {
            ConfigurationNode config = ConfigUtil.getConfig();
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (config == null || cachedConfig == null) {
                return null;
            }

            if (!config.node("stats", "usage").getBoolean(true)) {
                return null;
            }

            Map<String, Integer> retVal = new HashMap<>();
            for (StorageService service : cachedConfig.getStorage()) {
                retVal.compute(service.getClass().getSimpleName(), (k, v) -> {
                    if (v == null) {
                        return 1;
                    }
                    return v + 1;
                });
            }
            if (retVal.isEmpty()) {
                retVal.put("None", 1);
            }

            return retVal;
        }));
        metrics.addCustomChart(new Metrics.AdvancedPie("messaging", () -> {
            ConfigurationNode config = ConfigUtil.getConfig();
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (config == null || cachedConfig == null) {
                return null;
            }

            if (!config.node("stats", "usage").getBoolean(true)) {
                return null;
            }

            Map<String, Integer> retVal = new HashMap<>();
            for (MessagingService service : cachedConfig.getMessaging()) {
                retVal.compute(service.getClass().getSimpleName(), (k, v) -> {
                    if (v == null) {
                        return 1;
                    }
                    return v + 1;
                });
            }
            if (retVal.isEmpty()) {
                retVal.put("None", 1);
            }

            return retVal;
        }));
        metrics.addCustomChart(new Metrics.AdvancedPie("sources", () -> {
            ConfigurationNode config = ConfigUtil.getConfig();
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (config == null || cachedConfig == null) {
                return null;
            }

            if (!config.node("stats", "usage").getBoolean(true)) {
                return null;
            }

            Map<String, Integer> retVal = new HashMap<>();
            List<Source<? extends SourceModel>> sources = VPNAPIProvider.getInstance().getSourceManager().getSources();
            for (Source<? extends SourceModel> source : sources) {
                retVal.compute(source.getName(), (k, v) -> {
                    if (v == null) {
                        return 1;
                    }
                    return v + 1;
                });
            }
            if (retVal.isEmpty()) {
                retVal.put("None", 1);
            }

            return retVal;
        }));
        metrics.addCustomChart(new Metrics.SimplePie("algorithm", () -> {
            ConfigurationNode config = ConfigUtil.getConfig();
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (config == null || cachedConfig == null) {
                return null;
            }

            if (!config.node("stats", "usage").getBoolean(true)) {
                return null;
            }

            return cachedConfig.getVPNAlgorithmMethod().getName();
        }));
        metrics.addCustomChart(new Metrics.SimplePie("vpn_action", () -> {
            ConfigurationNode config = ConfigUtil.getConfig();
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (config == null || cachedConfig == null) {
                return null;
            }

            if (!config.node("stats", "usage").getBoolean(true)) {
                return null;
            }

            if (!cachedConfig.getVPNKickMessage().isEmpty() && !cachedConfig.getVPNActionCommands().isEmpty()) {
                return "multi";
            } else if (!cachedConfig.getVPNKickMessage().isEmpty()) {
                return "kick";
            } else if (!cachedConfig.getVPNActionCommands().isEmpty()) {
                return "commands";
            }
            return "none";
        }));
        metrics.addCustomChart(new Metrics.SimplePie("mcleaks_action", () -> {
            ConfigurationNode config = ConfigUtil.getConfig();
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (config == null || cachedConfig == null) {
                return null;
            }

            if (!config.node("stats", "usage").getBoolean(true)) {
                return null;
            }

            if (!cachedConfig.getMCLeaksKickMessage().isEmpty() && !cachedConfig.getMCLeaksActionCommands().isEmpty()) {
                return "multi";
            } else if (!cachedConfig.getMCLeaksKickMessage().isEmpty()) {
                return "kick";
            } else if (!cachedConfig.getMCLeaksActionCommands().isEmpty()) {
                return "commands";
            }
            return "none";
        }));
    }

    private void checkUpdate() {
        ConfigurationNode config = ConfigUtil.getConfig();
        if (config == null) {
            return;
        }

        BungeeUpdater updater;
        try {
            updater = ServiceLocator.get(BungeeUpdater.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (!config.node("update", "check").getBoolean(true)) {
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
        VPNAPI api = VPNAPIProvider.getInstance();
        api.getEventBus().post(new GenericAPIDisableEvent(api)).now();
        APIRegistrationUtil.deregister();

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig != null) {
            for (MessagingService service : cachedConfig.getMessaging()) {
                service.close();
            }
            for (StorageService service : cachedConfig.getStorage()) {
                service.close();
            }
        }

        Set<? extends MessagingHandler> messagingHandlers = ServiceLocator.remove(MessagingHandler.class);
        for (MessagingHandler handler : messagingHandlers) {
            handler.cancel();
        }
    }
}

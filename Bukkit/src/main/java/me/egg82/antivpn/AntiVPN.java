package me.egg82.antivpn;

import co.aikar.commands.*;
import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChainFactory;
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
import me.egg82.antivpn.api.APIRegistrationUtil;
import me.egg82.antivpn.api.GenericVPNAPI;
import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.GenericIPManager;
import me.egg82.antivpn.api.model.player.BukkitPlayerManager;
import me.egg82.antivpn.api.model.source.GenericSourceManager;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.api.platform.BukkitPlatform;
import me.egg82.antivpn.api.platform.BukkitPluginMetadata;
import me.egg82.antivpn.api.platform.Platform;
import me.egg82.antivpn.api.platform.PluginMetadata;
import me.egg82.antivpn.bukkit.BukkitEnvironmentUtil;
import me.egg82.antivpn.bukkit.BukkitVersionUtil;
import me.egg82.antivpn.commands.AntiVPNCommand;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.config.ConfigurationFileUtil;
import me.egg82.antivpn.events.EventHolder;
import me.egg82.antivpn.events.PlayerEvents;
import me.egg82.antivpn.events.PlayerLoginUpdateNotifyHandler;
import me.egg82.antivpn.hooks.*;
import me.egg82.antivpn.lang.LanguageFileUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.lang.PluginMessageFormatter;
import me.egg82.antivpn.messaging.GenericMessagingHandler;
import me.egg82.antivpn.messaging.MessagingHandler;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.messaging.ServerIDUtil;
import me.egg82.antivpn.services.GameAnalyticsErrorHandler;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.events.BukkitEventSubscriber;
import ninja.egg82.events.BukkitEvents;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import ninja.egg82.updater.SpigotUpdater;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;

public class AntiVPN {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ExecutorService workPool = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("AntiVPN-%d").build());

    private TaskChainFactory taskFactory;
    private PaperCommandManager commandManager;

    private final List<EventHolder> eventHolders = new ArrayList<>();
    private final List<BukkitEventSubscriber<?>> events = new ArrayList<>();
    private final IntList tasks = new IntArrayList();

    private final Plugin plugin;
    private final boolean isBukkit;

    private CommandIssuer consoleCommandIssuer = null;

    public AntiVPN(@NonNull Plugin plugin) {
        this.plugin = plugin;
        isBukkit = BukkitEnvironmentUtil.getEnvironment() == BukkitEnvironmentUtil.Environment.BUKKIT;
    }

    public void onLoad() {
        if (BukkitEnvironmentUtil.getEnvironment() != BukkitEnvironmentUtil.Environment.PAPER) {
            log(Level.INFO, ChatColor.AQUA + "====================================");
            log(Level.INFO, ChatColor.YELLOW + "Anti-VPN runs better on Paper!");
            log(Level.INFO, ChatColor.YELLOW + "https://whypaper.emc.gs/");
            log(Level.INFO, ChatColor.AQUA + "====================================");
        }

        if (!BukkitVersionUtil.isAtLeast("1.8")) {
            log(Level.INFO, ChatColor.GOLD + "====================================");
            log(Level.INFO, ChatColor.DARK_RED + "This plugin will likely not work on servers < 1.8");
            log(Level.INFO, ChatColor.GOLD + "====================================");
        }

        if (BukkitVersionUtil.getGameVersion().startsWith("1.8")) {
            log(Level.INFO, ChatColor.AQUA + "====================================");
            log(Level.INFO, ChatColor.DARK_RED + "DEAR LORD why are you on 1.8???");
            log(Level.INFO, ChatColor.DARK_RED + "Have you tried ViaVersion or ProtocolSupport lately?");
            log(Level.INFO, ChatColor.AQUA + "====================================");
        }
    }

    public void onEnable() {
        GameAnalyticsErrorHandler.open(ServerIDUtil.getId(new File(plugin.getDataFolder(), "stats-id.txt")), plugin.getDescription().getVersion(), Bukkit.getVersion());

        taskFactory = BukkitTaskChainFactory.create(plugin);
        commandManager = new PaperCommandManager(plugin);
        commandManager.enableUnstableAPI("help");

        consoleCommandIssuer = commandManager.getCommandIssuer(plugin.getServer().getConsoleSender());

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

        taskFactory.shutdown(4, TimeUnit.SECONDS);
        commandManager.unregisterCommands();

        for (int task : tasks) {
            Bukkit.getScheduler().cancelTask(task);
        }
        tasks.clear();

        for (EventHolder eventHolder : eventHolders) {
            eventHolder.cancel();
        }
        eventHolders.clear();
        for (BukkitEventSubscriber<?> event : events) {
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

        BukkitLocales locales = commandManager.getLocales();

        try {
            for (Locale locale : Locale.getAvailableLocales()) {
                Optional<File> localeFile = LanguageFileUtil.getLanguage(plugin.getDataFolder(), locale);
                if (localeFile.isPresent()) {
                    commandManager.addSupportedLanguage(locale);
                    locales.loadYamlLanguageFile(localeFile.get(), locale);
                }
            }
        } catch (IOException | InvalidConfigurationException ex) {
            logger.error(ex.getMessage(), ex);
        }

        locales.loadLanguages();
        locales.setDefaultLocale(cachedConfig.getLanguage());
        commandManager.usePerIssuerLocale(true, true);

        commandManager.setFormat(MessageType.ERROR, new PluginMessageFormatter(commandManager, Message.GENERAL__HEADER));
        commandManager.setFormat(MessageType.INFO, new PluginMessageFormatter(commandManager, Message.GENERAL__HEADER));
        commandManager.setFormat(MessageType.ERROR, ChatColor.DARK_RED, ChatColor.YELLOW, ChatColor.AQUA, ChatColor.WHITE);
        commandManager.setFormat(MessageType.INFO, ChatColor.WHITE, ChatColor.YELLOW, ChatColor.AQUA, ChatColor.GREEN, ChatColor.RED, ChatColor.GOLD, ChatColor.BLUE, ChatColor.GRAY, ChatColor.DARK_RED);
    }

    private void loadServices() {
        SourceManager sourceManager = new GenericSourceManager();

        ConfigurationFileUtil.reloadConfig(plugin.getDataFolder(), consoleCommandIssuer, messagingHandler, sourceManager);

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        GenericIPManager ipManager = new GenericIPManager(sourceManager, cachedConfig.getCacheTime().getTime(), cachedConfig.getCacheTime().getUnit());
        BukkitPlayerManager playerManager = new BukkitPlayerManager(cachedConfig.getThreads(), cachedConfig.getMcLeaksKey(), cachedConfig.getCacheTime().getTime(), cachedConfig.getCacheTime().getUnit());
        Platform platform = new BukkitPlatform(System.currentTimeMillis());
        PluginMetadata metadata = new BukkitPluginMetadata(plugin.getDescription().getVersion());
        VPNAPI api = new GenericVPNAPI(platform, metadata, ipManager, playerManager, sourceManager, cachedConfig);

        MessagingHandler messagingHandler = new GenericMessagingHandler(ipManager, playerManager);
        ServiceLocator.register(messagingHandler);

        ServiceLocator.register(new SpigotUpdater(plugin, 58291));

        APIRegistrationUtil.register(api);
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
                if (service.getClass().getSimpleName().equalsIgnoreCase(v)) {
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
                String ss = service.getClass().getSimpleName();
                if (ss.toLowerCase().startsWith(lower)) {
                    retVal.add(ss);
                }
            }
            return ImmutableList.copyOf(retVal);
        });

        commandManager.getCommandCompletions().registerCompletion("player", c -> {
            String lower = c.getInput().toLowerCase();
            Set<String> players = new LinkedHashSet<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (lower.isEmpty() || p.getName().toLowerCase().startsWith(lower)) {
                    Player player = c.getPlayer();
                    if (c.getSender().isOp() || (player != null && player.canSee(p) && !isVanished(p))) {
                        players.add(p.getName());
                    }
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

        commandManager.registerCommand(new AntiVPNCommand(plugin, taskFactory));
    }

    private void loadEvents() {
        events.add(BukkitEvents.subscribe(plugin, PlayerLoginEvent.class, EventPriority.LOW).handler(e -> new PlayerLoginUpdateNotifyHandler(plugin, commandManager).accept(e)));
        eventHolders.add(new PlayerEvents(plugin, consoleCommandIssuer));
    }

    private void loadTasks() {
        tasks.add(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> VPNAPIProvider.getInstance().runUpdateTask(), 1L, 10L).getTaskId());
    }

    private void loadHooks() {
        PluginManager manager = plugin.getServer().getPluginManager();

        Plugin plan;
        if ((plan = manager.getPlugin("Plan")) != null) {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_ENABLE, "{plugin}", "Plan");
            PlayerAnalyticsHook.create(plugin, plan);
        } else {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_DISABLE, "{plugin}", "Plan");
        }

        if (manager.getPlugin("PlaceholderAPI") != null) {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_ENABLE, "{plugin}", "PlaceholderAPI");
            ServiceLocator.register(new PlaceholderAPIHook());
        } else {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_DISABLE, "{plugin}", "PlaceholderAPI");
        }

        Plugin luckperms;
        if ((luckperms = manager.getPlugin("LuckPerms")) != null) {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_ENABLE, "{plugin}", "LuckPerms");
            if (ConfigUtil.getDebugOrFalse()) {
                consoleCommandIssuer.sendMessage(LogUtil.HEADING + "<c2>Running actions on async pre-login.</c2>");
            }
            LuckPermsHook.create(plugin, luckperms, consoleCommandIssuer);
        } else {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_DISABLE, "{plugin}", "LuckPerms");
            Plugin vault;
            if ((vault = manager.getPlugin("Vault")) != null) {
                consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_ENABLE, "{plugin}", "Vault");
                if (ConfigUtil.getDebugOrFalse()) {
                    consoleCommandIssuer.sendMessage(LogUtil.HEADING + "<c2>Running actions on async pre-login.</c2>");
                }
                VaultHook.create(plugin, vault, consoleCommandIssuer);
            } else {
                consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_DISABLE, "{plugin}", "Vault");
                if (ConfigUtil.getDebugOrFalse()) {
                    consoleCommandIssuer.sendMessage(LogUtil.HEADING + "<c2>Running actions on sync login.</c2>");
                }
            }
        }
    }

    private static final AtomicLong blockedVPNs = new AtomicLong(0L);

    private static final AtomicLong blockedMCLeaks = new AtomicLong(0L);

    public static void incrementBlockedVPNs() { blockedVPNs.getAndIncrement(); }

    public static void incrementBlockedMCLeaks() { blockedMCLeaks.getAndIncrement(); }

    private void loadMetrics() {
        Metrics metrics = new Metrics(plugin, 3249); // TODO: Change ID when bStats finally allows multiple plugins of the same name
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

        SpigotUpdater updater;
        try {
            updater = ServiceLocator.get(SpigotUpdater.class);
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
        APIRegistrationUtil.deregister();

        Set<? extends MessagingHandler> messagingHandlers = ServiceLocator.remove(MessagingHandler.class);
        for (MessagingHandler handler : messagingHandlers) {
            handler.cancel();
        }
    }

    private void log(@NonNull Level level, @NonNull String message) {
        plugin.getServer().getLogger().log(level, (isBukkit) ? ChatColor.stripColor(message) : message);
    }

    private boolean isVanished(@NonNull Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }
}

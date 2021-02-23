package me.egg82.antivpn;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import me.egg82.antivpn.api.*;
import me.egg82.antivpn.api.event.api.GenericAPIDisableEvent;
import me.egg82.antivpn.api.event.api.GenericAPILoadedEvent;
import me.egg82.antivpn.api.event.api.GenericPublicationErrorHandler;
import me.egg82.antivpn.api.model.ip.BukkitIPManager;
import me.egg82.antivpn.api.model.player.BukkitPlayerManager;
import me.egg82.antivpn.api.model.source.GenericSourceManager;
import me.egg82.antivpn.api.platform.BukkitPlatform;
import me.egg82.antivpn.api.platform.BukkitPluginMetadata;
import me.egg82.antivpn.api.platform.Platform;
import me.egg82.antivpn.api.platform.PluginMetadata;
import me.egg82.antivpn.bukkit.BukkitEnvironmentUtil;
import me.egg82.antivpn.bukkit.BukkitVersionUtil;
import me.egg82.antivpn.commands.AntiVPNCommands;
import me.egg82.antivpn.commands.CommandHolder;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.config.ConfigurationFileUtil;
import me.egg82.antivpn.events.EarlyCheckEvents;
import me.egg82.antivpn.events.EventHolder;
import me.egg82.antivpn.events.ExtraPlayerEvents;
import me.egg82.antivpn.events.LateCheckEvents;
import me.egg82.antivpn.hooks.*;
import me.egg82.antivpn.lang.BukkitLocaleCommandUtil;
import me.egg82.antivpn.lang.BukkitLocalizedCommandSender;
import me.egg82.antivpn.lang.I18NManager;
import me.egg82.antivpn.lang.MessageKey;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.messaging.GenericMessagingHandler;
import me.egg82.antivpn.messaging.MessagingHandler;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.messaging.ServerIDUtil;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.utils.VersionUtil;
import net.engio.mbassy.bus.MBassador;
import ninja.egg82.events.BukkitEventSubscriber;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AntiVPN {
    private static final Logger logger = LoggerFactory.getLogger(AntiVPN.class);

    private final List<CommandHolder> commandHolders = new ArrayList<>();
    private final List<EventHolder> eventHolders = new ArrayList<>();
    private final List<BukkitEventSubscriber<?>> events = new ArrayList<>();
    private final IntList tasks = new IntArrayList();

    private final JavaPlugin plugin;

    public AntiVPN(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void onLoad() { }

    public void onEnable() {
        GELFLogger.setData(ServerIDUtil.getId(new File(plugin.getDataFolder(), "stats-id.txt")), plugin.getDescription().getVersion(), Platform.Type.BUKKIT, Bukkit.getVersion());

        BukkitLocaleCommandUtil.create(plugin);
        BukkitLocalizedCommandSender console = BukkitLocaleCommandUtil.getConsole();

        if (BukkitEnvironmentUtil.getEnvironment() != BukkitEnvironmentUtil.Environment.PAPER) {
            console.sendMessage(MessageKey.BANNER__USE_PAPER);
        }
        if (!VersionUtil.isAtLeast("1.8", '.', BukkitVersionUtil.getGameVersion(), '.')) {
            console.sendMessage(MessageKey.BANNER__PRE_18);
        }
        if (BukkitVersionUtil.getGameVersion().startsWith("1.8")) {
            console.sendMessage(MessageKey.BANNER__IN_18);
        }

        loadServices();
        loadHooks();
        loadCommands();
        loadEvents();
        loadTasks();

        int numCommands = 0;
        for (CommandHolder commandHolder : commandHolders) {
            numCommands += commandHolder.numCommands();
        }

        int numEvents = events.size();
        for (EventHolder eventHolder : eventHolders) {
            numEvents += eventHolder.numEvents();
        }

        console.sendMessage(MessageKey.GENERAL__ENABLE_MESSAGE);

        console.sendMessage(MessageKey.GENERAL__LOAD_MESSAGE,
                "{version}", plugin.getDescription().getVersion(),
                "{apiversion}", VPNAPIProvider.getInstance().getPluginMetadata().getApiVersion(),
                "{commands}", String.valueOf(numCommands),
                "{events}", String.valueOf(numEvents),
                "{tasks}", String.valueOf(tasks.size())
        );
    }

    public void onDisable() {
        for (CommandHolder commandHolder : commandHolders) {
            commandHolder.cancel();
        }
        commandHolders.clear();

        for (int task : tasks) {
            Bukkit.getScheduler().cancelTask(task);
        }
        tasks.clear();

        try {
            VPNAPIProvider.getInstance().runUpdateTask().join();
        } catch (CancellationException | CompletionException ex) {
            GELFLogger.exception(logger, ex, BukkitLocaleCommandUtil.getConsole().getLocalizationManager());
        }

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

        BukkitLocaleCommandUtil.getConsole().sendMessage(MessageKey.GENERAL__DISABLE_MESSAGE);

        BukkitLocaleCommandUtil.close();
    }

    private void loadServices() {
        GenericSourceManager sourceManager = new GenericSourceManager();
        MessagingHandler messagingHandler = new GenericMessagingHandler();
        ConfigurationFileUtil.reloadConfig(plugin.getDataFolder(), BukkitLocaleCommandUtil.getConsole(), messagingHandler, sourceManager);
        I18NManager.clearCaches();

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        BukkitLocaleCommandUtil.setConsoleLocale(plugin, cachedConfig.getLanguage());

        BukkitIPManager ipManager = new BukkitIPManager(plugin, sourceManager, cachedConfig.getCacheTime());
        BukkitPlayerManager playerManager = new BukkitPlayerManager(plugin, cachedConfig.getMcLeaksKey(), cachedConfig.getCacheTime());
        Platform platform = new BukkitPlatform(System.currentTimeMillis());
        PluginMetadata metadata = new BukkitPluginMetadata(plugin.getDescription().getVersion());
        VPNAPI api = new GenericVPNAPI(platform, metadata, ipManager, playerManager, sourceManager, cachedConfig, new MBassador<>(new GenericPublicationErrorHandler(BukkitLocaleCommandUtil.getConsole().getLocalizationManager())));

        APIUtil.setManagers(ipManager, playerManager, sourceManager);
        APIRegistrationUtil.register(api);
        api.getEventBus().post(new GenericAPILoadedEvent(api)).now();
    }

    private void loadCommands() {
        // TODO: Clean up this mess
        /*commandManager.getCommandConditions().addCondition(String.class, "ip", (c, exec, value) -> {
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
        });*/

        commandHolders.add(new AntiVPNCommands(plugin));
    }

    private void loadEvents() {
        eventHolders.add(new EarlyCheckEvents(plugin));
        eventHolders.add(new LateCheckEvents(plugin));
        eventHolders.add(new ExtraPlayerEvents(plugin));
    }

    private void loadTasks() {
        tasks.add(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                VPNAPIProvider.getInstance().runUpdateTask().join();
            } catch (CancellationException | CompletionException ex) {
                GELFLogger.exception(logger, ex, BukkitLocaleCommandUtil.getConsole().getLocalizationManager());
            }
        }, 1L, 20L).getTaskId());
    }

    private void loadHooks() {
        BukkitLocalizedCommandSender console = BukkitLocaleCommandUtil.getConsole();
        PluginManager manager = plugin.getServer().getPluginManager();

        console.sendMessage(MessageKey.GENERAL__ENABLE_HOOK, "{hook}", "BStats");
        BStatsHook.create(plugin, 10438);

        console.sendMessage(MessageKey.GENERAL__ENABLE_HOOK, "{hook}", "Updater");
        UpdaterHook.create(plugin, 58291);

        Plugin plan;
        if ((plan = manager.getPlugin("Plan")) != null) {
            console.sendMessage(MessageKey.GENERAL__ENABLE_HOOK, "{hook}", "Plan");
            PlayerAnalyticsHook.create(plugin, plan);
        } else {
            console.sendMessage(MessageKey.GENERAL__NO_HOOK, "{hook}", "Plan");
        }

        Plugin placeholderapi;
        if ((placeholderapi = manager.getPlugin("PlaceholderAPI")) != null) {
            console.sendMessage(MessageKey.GENERAL__ENABLE_HOOK, "{hook}", "PlaceholderAPI");
            PlaceholderAPIHook.create(plugin, placeholderapi);
        } else {
            console.sendMessage(MessageKey.GENERAL__NO_HOOK, "{hook}", "PlaceholderAPI");
        }

        Plugin luckperms;
        if ((luckperms = manager.getPlugin("LuckPerms")) != null) {
            console.sendMessage(MessageKey.GENERAL__ENABLE_HOOK, "{hook}", "LuckPerms");
            if (ConfigUtil.getDebugOrFalse()) {
                console.sendMessage(MessageKey.GENERAL__ASYNC_ACTIONS);
            }
            LuckPermsHook.create(plugin, luckperms);
        } else {
            console.sendMessage(MessageKey.GENERAL__NO_HOOK, "{hook}", "LuckPerms");
            Plugin vault;
            if ((vault = manager.getPlugin("Vault")) != null) {
                console.sendMessage(MessageKey.GENERAL__ENABLE_HOOK, "{hook}", "Vault");
                if (ConfigUtil.getDebugOrFalse()) {
                    console.sendMessage(MessageKey.GENERAL__ASYNC_ACTIONS);
                }
                VaultHook.create(plugin, vault);
            } else {
                console.sendMessage(MessageKey.GENERAL__NO_HOOK, "{hook}", "Vault");
                if (ConfigUtil.getDebugOrFalse()) {
                    console.sendMessage(MessageKey.GENERAL__SYNC_ACTIONS);
                }
            }
        }
    }

    private void unloadHooks() {
        for (Iterator<PluginHook> i = PluginHooks.getHooks().iterator(); i.hasNext();) {
            i.next().cancel();
            i.remove();
        }
    }

    public void unloadServices() {
        VPNAPI api = VPNAPIProvider.getInstance();
        api.getEventBus().post(new GenericAPIDisableEvent(api)).now();
        api.getEventBus().shutdown();
        APIRegistrationUtil.deregister();

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        for (MessagingService service : cachedConfig.getMessaging()) {
            service.close();
        }
        for (StorageService service : cachedConfig.getStorage()) {
            service.close();
        }
    }

    private boolean isVanished(@NotNull Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }
}

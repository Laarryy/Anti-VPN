package me.egg82.antivpn;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.egg82.antivpn.api.APIRegistrationUtil;
import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.VPNAPIImpl;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.event.VPNEvent;
import me.egg82.antivpn.api.event.api.APIDisableEventImpl;
import me.egg82.antivpn.api.event.api.APILoadedEventImpl;
import me.egg82.antivpn.api.model.ip.BukkitIPManager;
import me.egg82.antivpn.api.model.player.BukkitPlayerManager;
import me.egg82.antivpn.api.model.source.SourceManagerImpl;
import me.egg82.antivpn.api.platform.BukkitPlatform;
import me.egg82.antivpn.api.platform.BukkitPluginMetadata;
import me.egg82.antivpn.api.platform.Platform;
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
import me.egg82.antivpn.locale.*;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.messaging.PacketManager;
import me.egg82.antivpn.messaging.handler.MessagingHandler;
import me.egg82.antivpn.messaging.handler.MessagingHandlerImpl;
import me.egg82.antivpn.messaging.packets.MultiPacket;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.messaging.packets.server.InitializationPacket;
import me.egg82.antivpn.messaging.packets.server.ShutdownPacket;
import me.egg82.antivpn.messaging.packets.vpn.DeleteIPPacket;
import me.egg82.antivpn.messaging.packets.vpn.DeletePlayerPacket;
import me.egg82.antivpn.messaging.packets.vpn.IPPacket;
import me.egg82.antivpn.messaging.packets.vpn.PlayerPacket;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.utils.EventUtil;
import me.egg82.antivpn.utils.PacketUtil;
import me.egg82.antivpn.utils.VersionUtil;
import me.egg82.antivpn.api.platform.AbstractPluginMetadata;
import net.kyori.event.SimpleEventBus;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

public class AntiVPN {
    private static final Logger logger = new GELFLogger(LoggerFactory.getLogger(AntiVPN.class));

    private final List<CommandHolder> commandHolders = new ArrayList<>();
    private final List<EventHolder> eventHolders = new ArrayList<>();
    private final List<BukkitEventSubscriber<?>> events = new ArrayList<>();
    private final IntList tasks = new IntArrayList();

    private final JavaPlugin plugin;

    public AntiVPN(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        loadPackets();
    }

    public void onLoad() {
        // Empty
    }

    public void onEnable() {
        BukkitLocaleCommandUtil.create(plugin);
        BukkitLocalizedCommandSender console = BukkitLocaleCommandUtil.getConsole();

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

    private void loadPackets() {
        PacketManager.register(InitializationPacket.class, InitializationPacket::new); // Ensure InitializationPacket always has a packet ID of 1
        PacketManager.register(MultiPacket.class, MultiPacket::new); // Ensure MultiPacket always has a packet ID of 2

        PacketManager.register(ShutdownPacket.class, ShutdownPacket::new);

        PacketManager.register(DeleteIPPacket.class, DeleteIPPacket::new);
        PacketManager.register(DeletePlayerPacket.class, DeletePlayerPacket::new);
        PacketManager.register(IPPacket.class, IPPacket::new);
        PacketManager.register(PlayerPacket.class, PlayerPacket::new);
    }

    private void loadServices() {
        SourceManagerImpl sourceManager = new SourceManagerImpl();
        MessagingHandler messagingHandler = new MessagingHandlerImpl();
        ConfigurationFileUtil.reloadConfig(plugin.getDataFolder(), BukkitLocaleCommandUtil.getConsole(), messagingHandler, sourceManager);
        I18NManager.clearCaches();

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        BukkitLocaleCommandUtil.setConsoleLocale(plugin, LocaleUtil.getDefaultI18N());

        BukkitIPManager ipManager = new BukkitIPManager(plugin, sourceManager, cachedConfig.getCacheTime());
        BukkitPlayerManager playerManager = new BukkitPlayerManager(plugin, cachedConfig.getMcLeaksKey(), cachedConfig.getCacheTime());
        Platform platform = new BukkitPlatform(System.currentTimeMillis());
        AbstractPluginMetadata metadata = new BukkitPluginMetadata(plugin.getDescription().getVersion());
        VPNAPI api = new VPNAPIImpl(platform, metadata, ipManager, playerManager, sourceManager, new SimpleEventBus<>(VPNEvent.class));

        APIRegistrationUtil.register(api);
        EventUtil.post(new APILoadedEventImpl(api), api.getEventBus());

        PacketUtil.queuePacket(new InitializationPacket(ConfigUtil.getCachedConfig().getServerId(), Packet.VERSION));
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
                logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
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
        PluginHooks.getHooks().removeIf(h -> {
            h.cancel();
            return true;
        });
    }

    public void unloadServices() {
        VPNAPI api = VPNAPIProvider.getInstance();

        // Needs to be done before the final runUpdateTask()
        PacketUtil.queuePacket(new ShutdownPacket(ConfigUtil.getCachedConfig().getServerId()));

        try {
            api.runUpdateTask().join();
        } catch (CancellationException | CompletionException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }

        EventUtil.post(new APIDisableEventImpl(api), api.getEventBus());
        api.getEventBus().unregisterAll();
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
            if (meta.asBoolean()) {
                return true;
            }
        }
        return false;
    }
}

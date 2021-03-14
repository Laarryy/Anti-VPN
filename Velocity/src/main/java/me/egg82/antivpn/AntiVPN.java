package me.egg82.antivpn;

import co.aikar.commands.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import me.egg82.antivpn.api.APIRegistrationUtil;
import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.VPNAPIImpl;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.event.api.APIDisableEventImpl;
import me.egg82.antivpn.api.event.api.APILoadedEventImpl;
import me.egg82.antivpn.api.model.ip.VelocityIPManager;
import me.egg82.antivpn.api.model.player.VelocityPlayerManager;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.SourceManagerImpl;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.api.platform.Platform;
import me.egg82.antivpn.api.platform.PluginMetadata;
import me.egg82.antivpn.api.platform.VelocityPlatform;
import me.egg82.antivpn.api.platform.VelocityPluginMetadata;
import me.egg82.antivpn.commands.AntiVPNCommand;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.config.ConfigurationFileUtil;
import me.egg82.antivpn.events.EventHolder;
import me.egg82.antivpn.events.PlayerEvents;
import me.egg82.antivpn.hooks.LuckPermsHook;
import me.egg82.antivpn.hooks.PlayerAnalyticsHook;
import me.egg82.antivpn.hooks.PluginHook;
import me.egg82.antivpn.locale.LanguageFileUtil;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.locale.PluginMessageFormatter;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.messaging.ServerIDUtil;
import me.egg82.antivpn.messaging.handler.MessagingHandler;
import me.egg82.antivpn.messaging.handler.MessagingHandlerImpl;
import me.egg82.antivpn.services.GameAnalyticsErrorHandler;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.utils.ExceptionUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import net.engio.mbassy.bus.MBassador;
import net.kyori.text.format.TextColor;
import ninja.egg82.events.PriorityEventSubscriber;
import ninja.egg82.service.ServiceLocator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AntiVPN {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private VelocityCommandManager commandManager;

    private final List<EventHolder> eventHolders = new ArrayList<>();
    private final List<PriorityEventSubscriber<PostOrder, ?>> events = new ArrayList<>();
    private final List<ScheduledTask> tasks = new ArrayList<>();

    private final Object plugin;
    private final ProxyServer proxy;
    private final PluginDescription description;

    private CommandIssuer consoleCommandIssuer = null;

    public AntiVPN(@NotNull Object plugin, @NotNull ProxyServer proxy, @NotNull PluginDescription description) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.description = description;
    }

    public void onLoad() {
        // Empty
    }

    public void onEnable() {
        GameAnalyticsErrorHandler.open(ServerIDUtil.getId(new File(
                new File(description.getSource().get().getParent().toFile(), description.getName().get()),
                "stats-id.txt"
        )), description.getVersion().get(), proxy.getVersion().getVersion());

        commandManager = new VelocityCommandManager(proxy, plugin);
        commandManager.enableUnstableAPI("help");

        setChatColors();

        consoleCommandIssuer = commandManager.getCommandIssuer(proxy.getConsoleCommandSource());

        loadServices();
        loadLanguages();
        loadCommands();
        loadEvents();
        loadTasks();
        loadHooks();

        int numEvents = events.size();
        for (EventHolder eventHolder : eventHolders) {
            numEvents += eventHolder.numEvents();
        }

        consoleCommandIssuer.sendInfo(MessageKey.GENERAL__ENABLED);
        consoleCommandIssuer.sendInfo(MessageKey.GENERAL__LOAD,
                                      "{version}", description.getVersion().get(),
                                      "{apiversion}", VPNAPIProvider.getInstance().getPluginMetadata().getApiVersion(),
                                      "{commands}", String.valueOf(commandManager.getRegisteredRootCommands().size()),
                                      "{events}", String.valueOf(numEvents),
                                      "{tasks}", String.valueOf(tasks.size())
        );
    }

    public void onDisable() {
        commandManager.unregisterCommands();

        for (ScheduledTask task : tasks) {
            task.cancel();
        }
        tasks.clear();

        try {
            VPNAPIProvider.getInstance().runUpdateTask().join();
        } catch (CancellationException | CompletionException ex) {
            ExceptionUtil.handleException(ex, logger);
        }

        for (EventHolder eventHolder : eventHolders) {
            eventHolder.cancel();
        }
        eventHolders.clear();
        for (PriorityEventSubscriber<PostOrder, ?> event : events) {
            event.cancel();
        }
        events.clear();

        unloadHooks();
        unloadServices();

        consoleCommandIssuer.sendInfo(MessageKey.GENERAL__DISABLED);

        GameAnalyticsErrorHandler.close();
    }

    private void loadLanguages() {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        VelocityLocales locales = commandManager.getLocales();

        try {
            for (Locale locale : Locale.getAvailableLocales()) {
                Optional<File> localeFile = LanguageFileUtil.getLanguage(
                        new File(description.getSource().get().getParent().toFile(), description.getName().get()),
                        locale
                );
                if (localeFile.isPresent()) {
                    commandManager.addSupportedLanguage(locale);
                    loadYamlLanguageFile(locales, localeFile.get(), locale);
                }
            }
        } catch (IOException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }

        locales.loadLanguages();
        locales.setDefaultLocale(cachedConfig.getLanguage());
        commandManager.usePerIssuerLocale(true);

        commandManager.setFormat(MessageType.ERROR, new PluginMessageFormatter(commandManager, MessageKey.GENERAL__HEADER));
        commandManager.setFormat(MessageType.INFO, new PluginMessageFormatter(commandManager, MessageKey.GENERAL__HEADER));
        setChatColors();
    }

    private void setChatColors() {
        commandManager.setFormat(MessageType.ERROR, TextColor.DARK_RED, TextColor.YELLOW, TextColor.AQUA, TextColor.WHITE);
        commandManager.setFormat(
                MessageType.INFO,
                TextColor.WHITE,
                TextColor.YELLOW,
                TextColor.AQUA,
                TextColor.GREEN,
                TextColor.RED,
                TextColor.GOLD,
                TextColor.BLUE,
                TextColor.GRAY,
                TextColor.DARK_RED
        );
    }

    private void loadServices() {
        SourceManagerImpl sourceManager = new SourceManagerImpl();

        MessagingHandler messagingHandler = new MessagingHandlerImpl();
        ServiceLocator.register(messagingHandler);

        ConfigurationFileUtil.reloadConfig(
                new File(description.getSource().get().getParent().toFile(), description.getName().get()),
                consoleCommandIssuer,
                messagingHandler,
                sourceManager
        );

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        VelocityIPManager ipManager = new VelocityIPManager(proxy, sourceManager, cachedConfig.getCacheTime().getTime(), cachedConfig.getCacheTime().getUnit());
        VelocityPlayerManager playerManager = new VelocityPlayerManager(
                proxy,
                cachedConfig.getThreads(),
                cachedConfig.getMcLeaksKey(),
                cachedConfig.getCacheTime().getTime(),
                cachedConfig.getCacheTime().getUnit()
        );
        Platform platform = new VelocityPlatform(System.currentTimeMillis());
        PluginMetadata metadata = new VelocityPluginMetadata(proxy.getVersion().getVersion());
        VPNAPI api = new VPNAPIImpl(platform, metadata, ipManager, playerManager, sourceManager, cachedConfig, new MBassador<>(new GenericPublicationErrorHandler()));

        APIUtil.setManagers(ipManager, playerManager, sourceManager);

        APIRegistrationUtil.register(api);

        api.getEventBus().post(new APILoadedEventImpl(api)).now();
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
            for (Player p : proxy.getAllPlayers()) {
                if (lower.isEmpty() || p.getUsername().toLowerCase().startsWith(lower)) {
                    players.add(p.getUsername());
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

        commandManager.registerCommand(new AntiVPNCommand(proxy, description, consoleCommandIssuer));
    }

    private void loadEvents() {
        eventHolders.add(new PlayerEvents(plugin, proxy, consoleCommandIssuer));
    }

    private void loadTasks() {
        tasks.add(proxy.getScheduler().buildTask(plugin, () -> {
            try {
                VPNAPIProvider.getInstance().runUpdateTask().join();
            } catch (CancellationException | CompletionException ex) {
                ExceptionUtil.handleException(ex, logger);
            }
        }).delay(1L, TimeUnit.SECONDS).repeat(1L, TimeUnit.SECONDS).schedule());
    }

    private void loadHooks() {
        PluginManager manager = proxy.getPluginManager();

        if (manager.getPlugin("plan").isPresent()) {
            consoleCommandIssuer.sendInfo(MessageKey.GENERAL__HOOK_ENABLE, "{plugin}", "Plan");
            ServiceLocator.register(new PlayerAnalyticsHook(proxy));
        } else {
            consoleCommandIssuer.sendInfo(MessageKey.GENERAL__HOOK_DISABLE, "{plugin}", "Plan");
        }

        if (manager.getPlugin("luckperms").isPresent()) {
            consoleCommandIssuer.sendInfo(MessageKey.GENERAL__HOOK_ENABLE, "{plugin}", "LuckPerms");
            if (ConfigUtil.getDebugOrFalse()) {
                consoleCommandIssuer.sendMessage("<c2>Running actions on pre-login.</c2>");
            }
            ServiceLocator.register(new LuckPermsHook(consoleCommandIssuer));
        } else {
            consoleCommandIssuer.sendInfo(MessageKey.GENERAL__HOOK_DISABLE, "{plugin}", "LuckPerms");
            if (ConfigUtil.getDebugOrFalse()) {
                consoleCommandIssuer.sendMessage("<c2>Running actions on post-login.</c2>");
            }
        }
    }

    private static final AtomicLong blockedVPNs = new AtomicLong(0L);

    private static final AtomicLong blockedMCLeaks = new AtomicLong(0L);

    public static void incrementBlockedVPNs() {
        blockedVPNs.getAndIncrement();
    }

    public static void incrementBlockedMCLeaks() {
        blockedMCLeaks.getAndIncrement();
    }

    private void unloadHooks() {
        Set<? extends PluginHook> hooks = ServiceLocator.remove(PluginHook.class);
        for (PluginHook hook : hooks) {
            hook.cancel();
        }
    }

    public void unloadServices() {
        VPNAPI api = VPNAPIProvider.getInstance();
        api.getEventBus().post(new APIDisableEventImpl(api)).now();
        api.getEventBus().shutdown();
        APIRegistrationUtil.deregister();

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        for (MessagingService service : cachedConfig.getMessaging()) {
            service.close();
        }
        for (StorageService service : cachedConfig.getStorage()) {
            service.close();
        }

        Set<? extends MessagingHandler> messagingHandlers = ServiceLocator.remove(MessagingHandler.class);
        for (MessagingHandler handler : messagingHandlers) {
            handler.cancel();
        }
    }

    public boolean loadYamlLanguageFile(@NotNull VelocityLocales locales, @NotNull File file, @NotNull Locale locale) throws IOException {
        ConfigurationLoader<CommentedConfigurationNode> fileLoader = YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK).indent(2).file(file).build();
        return loadLanguage(locales, fileLoader.load(), locale);
    }

    private boolean loadLanguage(@NotNull VelocityLocales locales, @NotNull CommentedConfigurationNode config, @NotNull Locale locale) {
        boolean loaded = false;
        for (Map.Entry<Object, CommentedConfigurationNode> kvp : config.childrenMap().entrySet()) {
            for (Map.Entry<Object, CommentedConfigurationNode> kvp2 : kvp.getValue().childrenMap().entrySet()) {
                String value = kvp2.getValue().getString();
                if (value != null && !value.isEmpty()) {
                    locales.addMessage(locale, MessageKey.of(kvp.getKey() + "." + kvp2.getKey()), value);
                    loaded = true;
                }
            }
        }
        return loaded;
    }
}

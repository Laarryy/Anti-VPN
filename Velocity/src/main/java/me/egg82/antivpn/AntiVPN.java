package me.egg82.antivpn;

import co.aikar.commands.*;
import co.aikar.locales.MessageKey;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.io.File;
import java.io.IOException;
import java.util.*;
import me.egg82.antivpn.apis.SourceAPI;
import me.egg82.antivpn.commands.AntiVPNCommand;
import me.egg82.antivpn.enums.Message;
import me.egg82.antivpn.events.EventHolder;
import me.egg82.antivpn.events.PlayerEvents;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.hooks.PlayerAnalyticsHook;
import me.egg82.antivpn.hooks.PluginHook;
import me.egg82.antivpn.services.GameAnalyticsErrorHandler;
import me.egg82.antivpn.services.PluginMessageFormatter;
import me.egg82.antivpn.services.StorageMessagingHandler;
import me.egg82.antivpn.storage.Storage;
import me.egg82.antivpn.utils.*;
import net.kyori.text.format.TextColor;
import ninja.egg82.events.VelocityEventSubscriber;
import ninja.egg82.service.ServiceLocator;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;

public class AntiVPN {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private VelocityCommandManager commandManager;

    private List<EventHolder> eventHolders = new ArrayList<>();
    private List<VelocityEventSubscriber<?>> events = new ArrayList<>();
    private List<ScheduledTask> tasks = new ArrayList<>();

    private Object plugin;
    private ProxyServer proxy;
    private PluginDescription description;

    private CommandIssuer consoleCommandIssuer = null;

    public AntiVPN(Object plugin, ProxyServer proxy, PluginDescription description) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null.");
        }
        if (proxy == null) {
            throw new IllegalArgumentException("proxy cannot be null.");
        }
        if (description == null) {
            throw new IllegalArgumentException("description cannot be null.");
        }

        this.plugin = plugin;
        this.proxy = proxy;
        this.description = description;
    }

    public void onLoad() { }

    public void onEnable() {
        GameAnalyticsErrorHandler.open(ServerIDUtil.getID(new File(new File(description.getSource().get().getParent().toFile(), description.getName().get()), "stats-id.txt")), description.getVersion().get(), proxy.getVersion().getVersion());

        commandManager = new VelocityCommandManager(proxy, plugin);
        commandManager.enableUnstableAPI("help");

        consoleCommandIssuer = commandManager.getCommandIssuer(proxy.getConsoleCommandSource());

        loadLanguages();
        loadServices();
        loadCommands();
        loadEvents();
        loadTasks();
        loadHooks();

        int numEvents = events.size();
        for (EventHolder eventHolder : eventHolders) {
            numEvents += eventHolder.numEvents();
        }

        consoleCommandIssuer.sendInfo(Message.GENERAL__ENABLED);
        consoleCommandIssuer.sendInfo(Message.GENERAL__LOAD,
                "{version}", description.getVersion().get(),
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

        for (EventHolder eventHolder : eventHolders) {
            eventHolder.cancel();
        }
        eventHolders.clear();
        for (VelocityEventSubscriber<?> event : events) {
            event.cancel();
        }
        events.clear();

        unloadHooks();
        unloadServices();

        consoleCommandIssuer.sendInfo(Message.GENERAL__DISABLED);

        GameAnalyticsErrorHandler.close();
    }

    private void loadLanguages() {
        VelocityLocales locales = commandManager.getLocales();

        try {
            for (Locale locale : Locale.getAvailableLocales()) {
                Optional<File> localeFile = LanguageFileUtil.getLanguage(plugin, description, locale);
                if (localeFile.isPresent()) {
                    commandManager.addSupportedLanguage(locale);
                    loadYamlLanguageFile(locales, localeFile.get(), locale);
                }
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }

        locales.loadLanguages();
        commandManager.usePerIssuerLocale(true);

        commandManager.setFormat(MessageType.ERROR, new PluginMessageFormatter(commandManager, Message.GENERAL__HEADER));
        commandManager.setFormat(MessageType.INFO, new PluginMessageFormatter(commandManager, Message.GENERAL__HEADER));
        commandManager.setFormat(MessageType.ERROR, TextColor.DARK_RED, TextColor.YELLOW, TextColor.AQUA, TextColor.WHITE);
        commandManager.setFormat(MessageType.INFO, TextColor.WHITE, TextColor.YELLOW, TextColor.AQUA, TextColor.GREEN, TextColor.RED, TextColor.GOLD, TextColor.BLUE, TextColor.GRAY);
    }

    private void loadServices() {
        StorageMessagingHandler handler = new StorageMessagingHandler();
        ServiceLocator.register(handler);
        ConfigurationFileUtil.reloadConfig(plugin, proxy, description, handler, handler);
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
            for (Player p : proxy.getAllPlayers()) {
                if (lower.isEmpty() || p.getUsername().toLowerCase().startsWith(lower)) {
                    players.add(p.getUsername());
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

        commandManager.registerCommand(new AntiVPNCommand(plugin, proxy, description));
    }

    private void loadEvents() {
        eventHolders.add(new PlayerEvents(plugin, proxy));
    }

    private void loadTasks() { }

    private void loadHooks() {
        PluginManager manager = proxy.getPluginManager();

        if (manager.getPlugin("Plan").isPresent()) {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_ENABLE, "{plugin}", "Plan");
            ServiceLocator.register(new PlayerAnalyticsHook(proxy));
        } else {
            consoleCommandIssuer.sendInfo(Message.GENERAL__HOOK_DISABLE, "{plugin}", "Plan");
        }
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

    public boolean loadYamlLanguageFile(VelocityLocales locales, File file, Locale locale) throws IOException {
        ConfigurationLoader<ConfigurationNode> fileLoader = YAMLConfigurationLoader.builder().setFlowStyle(DumperOptions.FlowStyle.BLOCK).setIndent(2).setFile(file).build();
        return loadLanguage(locales, fileLoader.load(), locale);
    }

    private boolean loadLanguage(VelocityLocales locales, ConfigurationNode config, Locale locale) {
        boolean loaded = false;
        for (Map.Entry<Object, ? extends ConfigurationNode> kvp : config.getChildrenMap().entrySet()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> kvp2 : kvp.getValue().getChildrenMap().entrySet()) {
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

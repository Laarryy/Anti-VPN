package me.egg82.antivpn;

import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.RegisteredCommand;
import co.aikar.commands.VelocityCommandManager;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;

import java.io.File;
import java.util.*;
import me.egg82.antivpn.commands.AntiVPNCommand;
import me.egg82.antivpn.events.PostLoginCheckHandler;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.hooks.PlayerAnalyticsHook;
import me.egg82.antivpn.hooks.PluginHook;
import me.egg82.antivpn.services.GameAnalyticsErrorHandler;
import me.egg82.antivpn.utils.*;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import ninja.egg82.events.VelocityEventSubscriber;
import ninja.egg82.events.VelocityEvents;
import ninja.egg82.service.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AntiVPN {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private VelocityCommandManager commandManager;

    private List<VelocityEventSubscriber<?>> events = new ArrayList<>();

    private Object plugin;
    private ProxyServer proxy;
    private PluginDescription description;

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

    public void onLoad() {}

    public void onEnable() {
        GameAnalyticsErrorHandler.open(ServerIDUtil.getID(new File(new File(description.getSource().get().getParent().toFile(), description.getName().get()), "stats-id.txt")), description.getVersion().get(), proxy.getVersion().getVersion());

        commandManager = new VelocityCommandManager(proxy, plugin);
        commandManager.enableUnstableAPI("help");

        loadServices();
        loadCommands();
        loadEvents();
        loadHooks();

        proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Enabled").color(TextColor.GREEN)).build());

        proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading()
                .append(TextComponent.of("[").color(TextColor.YELLOW)).append(TextComponent.of("Version ").color(TextColor.AQUA)).append(TextComponent.of(description.getVersion().get()).color(TextColor.WHITE)).append(TextComponent.of("] ").color(TextColor.YELLOW))
                .append(TextComponent.of("[").color(TextColor.YELLOW)).append(TextComponent.of(String.valueOf(commandManager.getRegisteredRootCommands().size())).color(TextColor.WHITE)).append(TextComponent.of(" Commands").color(TextColor.GOLD)).append(TextComponent.of("] ").color(TextColor.YELLOW))
                .append(TextComponent.of("[").color(TextColor.YELLOW)).append(TextComponent.of(String.valueOf(events.size())).color(TextColor.WHITE)).append(TextComponent.of(" Events").color(TextColor.BLUE)).append(TextComponent.of("] ").color(TextColor.YELLOW))
                .build()
        );
    }

    public void onDisable() {
        commandManager.unregisterCommands();

        for (VelocityEventSubscriber<?> event : events) {
            event.cancel();
        }
        events.clear();

        unloadHooks();
        unloadServices();

        proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Disabled").color(TextColor.DARK_RED)).build());

        GameAnalyticsErrorHandler.close();
    }

    private void loadServices() {
        ConfigurationFileUtil.reloadConfig(plugin, proxy, description);

        ServiceUtil.registerWorkPool();
        ServiceUtil.registerRedis();
        ServiceUtil.registerRabbit();
        ServiceUtil.registerSQL();
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

        commandManager.registerCommand(new AntiVPNCommand(plugin, proxy, description));
    }

    private void loadEvents() {
        events.add(VelocityEvents.subscribe(plugin, proxy, PostLoginEvent.class, PostOrder.FIRST).handler(e -> new PostLoginCheckHandler(proxy).accept(e)));
    }

    private void loadHooks() {
        PluginManager manager = proxy.getPluginManager();

        if (manager.getPlugin("Plan").isPresent()) {
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Enabling support for Plan.").color(TextColor.GREEN)).build());
            ServiceLocator.register(new PlayerAnalyticsHook(proxy));
        } else {
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Plan was not found. Personal analytics support has been disabled.").color(TextColor.YELLOW)).build());
        }
    }

    private void unloadHooks() {
        Set<? extends PluginHook> hooks = ServiceLocator.remove(PluginHook.class);
        for (PluginHook hook : hooks) {
            hook.cancel();
        }
    }

    public void unloadServices() {
        ServiceUtil.unregisterWorkPool();
        ServiceUtil.unregisterRedis();
        ServiceUtil.unregisterRabbit();
        ServiceUtil.unregisterSQL();
    }
}

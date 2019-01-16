package me.egg82.antivpn;

import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.VelocityCommandManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import me.egg82.antivpn.commands.AntiVPNCommand;
import me.egg82.antivpn.core.SQLFetchResult;
import me.egg82.antivpn.enums.SQLType;
import me.egg82.antivpn.events.PostLoginCheckHandler;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.extended.RabbitMQReceiver;
import me.egg82.antivpn.extended.RedisSubscriber;
import me.egg82.antivpn.hooks.PlayerAnalyticsHook;
import me.egg82.antivpn.hooks.PluginHook;
import me.egg82.antivpn.services.Redis;
import me.egg82.antivpn.sql.MySQL;
import me.egg82.antivpn.sql.SQLite;
import me.egg82.antivpn.utils.ConfigurationFileUtil;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import ninja.egg82.events.VelocityEventSubscriber;
import ninja.egg82.events.VelocityEvents;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AntiVPN {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ExecutorService workPool = null;

    private VelocityCommandManager commandManager;

    private List<VelocityEventSubscriber<?>> events = new ArrayList<>();

    private VelocityBootstrap bootstrap;
    private ProxyServer proxy;
    private java.util.logging.Logger pluginLogger;
    private PluginDescription description;

    public AntiVPN(VelocityBootstrap bootstrap, ProxyServer proxy, java.util.logging.Logger pluginLogger, PluginDescription description) {
        this.bootstrap = bootstrap;
        this.proxy = proxy;
        this.pluginLogger = pluginLogger;
        this.description = description;
    }

    public void onLoad() {}

    public void onEnable() {
        commandManager = new VelocityCommandManager(bootstrap, description, proxy, pluginLogger);
        commandManager.enableUnstableAPI("help");

        loadServices();
        loadSQL();
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
    }

    private void loadServices() {
        ConfigurationFileUtil.reloadConfig(bootstrap, proxy, description);

        loadServicesExternal();
    }

    public void loadServicesExternal() {
        workPool = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat("AntiVPN-%d").build());

        Configuration config;
        CachedConfigValues cachedConfig;

        try {
            config = ServiceLocator.get(Configuration.class);
            cachedConfig = ServiceLocator.get(CachedConfigValues.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        workPool.submit(() -> new RedisSubscriber(cachedConfig.getRedisPool(), config.getNode("redis")));
        ServiceLocator.register(new RabbitMQReceiver(cachedConfig.getRabbitConnectionFactory()));
    }

    private void loadSQL() {
        Configuration config;
        CachedConfigValues cachedConfig;

        try {
            config = ServiceLocator.get(Configuration.class);
            cachedConfig = ServiceLocator.get(CachedConfigValues.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (cachedConfig.getSQLType() == SQLType.MySQL) {
            MySQL.createTables(cachedConfig.getSQL(), config.getNode("storage")).thenRun(() ->
                    MySQL.loadInfo(cachedConfig.getSQL(), config.getNode("storage")).thenAccept(v -> {
                        Redis.updateFromQueue(v, cachedConfig.getSourceCacheTime(), cachedConfig.getRedisPool(), config.getNode("redis"));
                        updateSQL();
                    })
            );
        } else if (cachedConfig.getSQLType() == SQLType.SQLite) {
            SQLite.createTables(cachedConfig.getSQL(), config.getNode("storage")).thenRun(() ->
                    SQLite.loadInfo(cachedConfig.getSQL(), config.getNode("storage")).thenAccept(v -> {
                        Redis.updateFromQueue(v, cachedConfig.getSourceCacheTime(), cachedConfig.getRedisPool(), config.getNode("redis"));
                        updateSQL();
                    })
            );
        }
    }

    public void loadSQLExternal() {
        Configuration config;
        CachedConfigValues cachedConfig;

        try {
            config = ServiceLocator.get(Configuration.class);
            cachedConfig = ServiceLocator.get(CachedConfigValues.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (cachedConfig.getSQLType() == SQLType.MySQL) {
            MySQL.createTables(cachedConfig.getSQL(), config.getNode("storage")).thenRun(() ->
                    MySQL.loadInfo(cachedConfig.getSQL(), config.getNode("storage")).thenAccept(v -> {
                        Redis.updateFromQueue(v, cachedConfig.getSourceCacheTime(), cachedConfig.getRedisPool(), config.getNode("redis"));
                    })
            );
        } else if (cachedConfig.getSQLType() == SQLType.SQLite) {
            SQLite.createTables(cachedConfig.getSQL(), config.getNode("storage")).thenRun(() ->
                    SQLite.loadInfo(cachedConfig.getSQL(), config.getNode("storage")).thenAccept(v -> {
                        Redis.updateFromQueue(v, cachedConfig.getSourceCacheTime(), cachedConfig.getRedisPool(), config.getNode("redis"));
                    })
            );
        }
    }

    private void updateSQL() {
        workPool.submit(() -> {
            try {
                Thread.sleep(10000L);
            } catch (InterruptedException ex) {
                logger.error(ex.getMessage(), ex);
                Thread.currentThread().interrupt();
            }

            Configuration config;
            CachedConfigValues cachedConfig;

            try {
                config = ServiceLocator.get(Configuration.class);
                cachedConfig = ServiceLocator.get(CachedConfigValues.class);
            } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
                logger.error(ex.getMessage(), ex);
                return;
            }

            SQLFetchResult result = null;

            try {
                if (cachedConfig.getSQLType() == SQLType.MySQL) {
                    result = MySQL.fetchQueue(cachedConfig.getSQL(), config.getNode("storage"), cachedConfig.getSourceCacheTime()).get();
                }

                if (result != null) {
                    Redis.updateFromQueue(result, cachedConfig.getSourceCacheTime(), cachedConfig.getRedisPool(), config.getNode("redis")).get();
                }
            } catch (ExecutionException ex) {
                logger.error(ex.getMessage(), ex);
            } catch (InterruptedException ex) {
                logger.error(ex.getMessage(), ex);
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
            CachedConfigValues cachedConfig;

            try {
                cachedConfig = ServiceLocator.get(CachedConfigValues.class);
            } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
                logger.error(ex.getMessage(), ex);
                return;
            }

            if (!cachedConfig.getSources().contains(value)) {
                throw new ConditionFailedException("Value must be a valid source name.");
            }
        });

        commandManager.registerCommand(new AntiVPNCommand(this, bootstrap, proxy, description));
    }

    private void loadEvents() {
        events.add(VelocityEvents.subscribe(bootstrap, proxy, PostLoginEvent.class, PostOrder.FIRST).handler(e -> new PostLoginCheckHandler(proxy).accept(e)));
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
        CachedConfigValues cachedConfig;
        RabbitMQReceiver rabbitReceiver;

        try {
            cachedConfig = ServiceLocator.get(CachedConfigValues.class);
            rabbitReceiver = ServiceLocator.get(RabbitMQReceiver.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (cachedConfig.getRedisPool() != null) {
            cachedConfig.getRedisPool().close();
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

        cachedConfig.getSQL().close();
    }
}

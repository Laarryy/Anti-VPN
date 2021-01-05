package me.egg82.antivpn.events;

import co.aikar.commands.CommandManager;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.Message;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Plugin;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import ninja.egg82.updater.BungeeUpdater;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;

public class PostLoginUpdateNotifyHandler implements Consumer<PostLoginEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Plugin plugin;
    private final CommandManager commandManager;

    public PostLoginUpdateNotifyHandler(@NonNull Plugin plugin, @NonNull CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public void accept(@NonNull PostLoginEvent event) {
        if (!event.getPlayer().hasPermission("avpn.admin")) {
            return;
        }

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

            if (config.node("update", "notify").getBoolean(true)) {
                try {
                    String version = updater.getLatestVersion().get();
                    commandManager.getCommandIssuer(event.getPlayer()).sendInfo(Message.GENERAL__UPDATE, "{version}", version);
                } catch (ExecutionException ex) {
                    logger.error(ex.getMessage(), ex);
                } catch (InterruptedException ex) {
                    logger.error(ex.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
}

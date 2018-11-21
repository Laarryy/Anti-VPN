package me.egg82.antivpn.events;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.utils.LogUtil;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import ninja.egg82.updater.SpigotUpdater;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerLoginUpdateNotifyHandler implements Consumer<PlayerLoginEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Plugin plugin;

    public PlayerLoginUpdateNotifyHandler(Plugin plugin) { this.plugin = plugin; }

    public void accept(PlayerLoginEvent event) {
        if (!event.getPlayer().hasPermission("avpn.admin")) {
            return;
        }

        Configuration config;
        SpigotUpdater updater;

        try {
            config = ServiceLocator.get(Configuration.class);
            updater = ServiceLocator.get(SpigotUpdater.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (!config.getNode("update", "check").getBoolean(true)) {
            return;
        }

        updater.isUpdateAvailable().thenAccept(v -> {
            if (!v) {
                return;
            }

            if (config.getNode("update", "notify").getBoolean(true)) {
                try {
                    String message = LogUtil.getHeading() + ChatColor.AQUA + " (Bukkit) has an " + ChatColor.GREEN + "update" + ChatColor.AQUA + " available! New version: " + ChatColor.YELLOW + updater.getLatestVersion().get();
                    Bukkit.getScheduler().runTask(plugin, () -> event.getPlayer().sendMessage(message));
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

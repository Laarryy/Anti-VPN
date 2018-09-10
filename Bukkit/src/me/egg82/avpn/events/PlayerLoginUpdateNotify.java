package me.egg82.avpn.events;

import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerJoinEvent;

import me.egg82.avpn.Config;
import ninja.egg82.bukkit.BasePlugin;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.handlers.events.HighEventHandler;

public class PlayerLoginUpdateNotify extends HighEventHandler<PlayerJoinEvent> {
    // vars
    private BasePlugin plugin = ServiceLocator.getService(BasePlugin.class);

    private String latestVersion = null;

    // constructor
    public PlayerLoginUpdateNotify() {
        super();
    }

    // public

    // private
    protected void onExecute(long elapsedMilliseconds) {
        if (!Config.notifyUpdates) {
            return;
        }

        if (!plugin.isUpdateAvailable()) {
            return;
        }

        if (latestVersion == null) {
            try {
                latestVersion = plugin.getLatestVersion();
            } catch (Exception ex) {
                plugin.printError("Could not get latest plugin version.");
                ex.printStackTrace();
                return;
            }
        }

        if (event.getPlayer().hasPermission("avpn.admin")) {
            event.getPlayer().sendMessage(ChatColor.AQUA + "Anti-VPN (Bukkit) has an " + ChatColor.GREEN + "update" + ChatColor.AQUA + " available! New version: " + ChatColor.YELLOW + latestVersion);
        }
    }
}

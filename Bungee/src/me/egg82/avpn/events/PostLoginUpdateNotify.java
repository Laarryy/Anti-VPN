package me.egg82.avpn.events;

import me.egg82.avpn.Config;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PostLoginEvent;
import ninja.egg82.bungeecord.BasePlugin;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.handlers.events.async.HighAsyncEventHandler;

public class PostLoginUpdateNotify extends HighAsyncEventHandler<PostLoginEvent> {
    // vars
    private BasePlugin plugin = ServiceLocator.getService(BasePlugin.class);

    private String latestVersion = null;

    // constructor
    public PostLoginUpdateNotify() {
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
            event.getPlayer().sendMessage(
                new TextComponent(ChatColor.AQUA + "Anti-VPN (Bungee) has an " + ChatColor.GREEN + "update" + ChatColor.AQUA + " available! New version: " + ChatColor.YELLOW + latestVersion));
        }
    }
}

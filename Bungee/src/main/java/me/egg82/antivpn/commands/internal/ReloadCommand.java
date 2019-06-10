package me.egg82.antivpn.commands.internal;

import me.egg82.antivpn.utils.ConfigurationFileUtil;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ServiceUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;

public class ReloadCommand implements Runnable {
    private final Plugin plugin;
    private final CommandSender sender;

    public ReloadCommand(Plugin plugin, CommandSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    public void run() {
        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.YELLOW + "Reloading, please wait.."));

        ServiceUtil.unregisterWorkPool();
        ServiceUtil.unregisterRedis();
        ServiceUtil.unregisterRabbit();
        ServiceUtil.unregisterSQL();
        ConfigurationFileUtil.reloadConfig(plugin);
        ServiceUtil.registerWorkPool();
        ServiceUtil.registerRedis();
        ServiceUtil.registerRabbit();
        ServiceUtil.registerSQL();

        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.GREEN + "Configuration reloaded!"));
    }
}

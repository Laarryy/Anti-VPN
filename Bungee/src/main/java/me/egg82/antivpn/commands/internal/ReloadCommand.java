package me.egg82.antivpn.commands.internal;

import me.egg82.antivpn.AntiVPN;
import me.egg82.antivpn.utils.ConfigurationFileUtil;
import me.egg82.antivpn.utils.LogUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;

public class ReloadCommand implements Runnable {
    private final AntiVPN concrete;
    private final Plugin plugin;
    private final CommandSender sender;

    public ReloadCommand(AntiVPN concrete, Plugin plugin, CommandSender sender) {
        this.concrete = concrete;
        this.plugin = plugin;
        this.sender = sender;
    }

    public void run() {
        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.YELLOW + "Reloading, please wait.."));
        concrete.unloadServices();
        ConfigurationFileUtil.reloadConfig(plugin);
        concrete.loadServicesExternal();
        concrete.loadSQLExternal();
        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.GREEN + "Configuration reloaded!"));
    }
}

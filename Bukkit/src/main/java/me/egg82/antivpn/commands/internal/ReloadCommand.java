package me.egg82.antivpn.commands.internal;

import co.aikar.taskchain.TaskChain;
import me.egg82.antivpn.AntiVPN;
import me.egg82.antivpn.utils.ConfigurationFileUtil;
import me.egg82.antivpn.utils.LogUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ReloadCommand implements Runnable {
    private final AntiVPN concrete;
    private final Plugin plugin;
    private final TaskChain<?> chain;
    private final CommandSender sender;

    public ReloadCommand(AntiVPN concrete, Plugin plugin, TaskChain<?> chain, CommandSender sender) {
        this.concrete = concrete;
        this.plugin = plugin;
        this.chain = chain;
        this.sender = sender;
    }

    public void run() {
        sender.sendMessage(LogUtil.getHeading() + ChatColor.YELLOW + "Reloading, please wait..");

        chain
                .async(concrete::unloadServices)
                .async(() -> ConfigurationFileUtil.reloadConfig(plugin))
                .async(concrete::loadServicesExternal)
                .async(concrete::loadSQLExternal)
                .sync(() -> sender.sendMessage(LogUtil.getHeading() + ChatColor.GREEN + "Configuration reloaded!"))
                .execute();
    }
}

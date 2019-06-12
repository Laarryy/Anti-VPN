package me.egg82.antivpn.commands.internal;

import co.aikar.taskchain.TaskChain;
import me.egg82.antivpn.utils.ConfigurationFileUtil;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ServiceUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ReloadCommand implements Runnable {
    private final Plugin plugin;
    private final TaskChain<?> chain;
    private final CommandSender sender;

    public ReloadCommand(Plugin plugin, TaskChain<?> chain, CommandSender sender) {
        this.plugin = plugin;
        this.chain = chain;
        this.sender = sender;
    }

    public void run() {
        sender.sendMessage(LogUtil.getHeading() + ChatColor.YELLOW + "Reloading, please wait..");

        chain
                .async(ServiceUtil::unregisterWorkPool)
                .async(ServiceUtil::unregisterRedis)
                .async(ServiceUtil::unregisterRabbit)
                .async(ServiceUtil::unregisterSQL)
                .async(() -> ConfigurationFileUtil.reloadConfig(plugin))
                .async(ServiceUtil::registerWorkPool)
                .async(ServiceUtil::registerRedis)
                .async(ServiceUtil::registerRabbit)
                .async(ServiceUtil::registerSQL)
                .sync(() -> sender.sendMessage(LogUtil.getHeading() + ChatColor.GREEN + "Configuration reloaded!"))
                .execute();
    }
}

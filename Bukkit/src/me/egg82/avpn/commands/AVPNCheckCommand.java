package me.egg82.avpn.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import me.egg82.avpn.Config;
import me.egg82.avpn.VPNAPI;
import me.egg82.avpn.utils.ValidationUtil;
import ninja.egg82.plugin.handlers.CommandHandler;
import ninja.egg82.utils.ThreadUtil;

public class AVPNCheckCommand extends CommandHandler {
    // vars
    private VPNAPI api = VPNAPI.getInstance();

    // constructor
    public AVPNCheckCommand() {
        super();
    }

    // public

    // private
    protected void onExecute(long elapsedMilliseconds) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Incorrect command usage!");
            String name = getClass().getSimpleName();
            name = name.substring(0, name.length() - 7).toLowerCase();
            Bukkit.getServer().dispatchCommand((CommandSender) sender.getHandle(), "? " + name);
            return;
        }
        if (!ValidationUtil.isValidIp(args[0])) {
            sender.sendMessage(ChatColor.RED + "The IP specified isn't valid!");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Checking IP..");
        ThreadUtil.submit(new Runnable() {
            public void run() {
                if (Config.consensus >= 0.0d) {
                    // Consensus algorithm
                    sender.sendMessage((api.consensus(args[0]) >= Config.consensus) ? ChatColor.RED + "VPN/Proxy detected" : ChatColor.GREEN + "No VPN/Proxy detected");
                } else {
                    // Cascade algorithm
                    sender.sendMessage(api.isVPN(args[0]) ? ChatColor.RED + "VPN/Proxy detected" : ChatColor.GREEN + "No VPN/Proxy detected");
                }
            }
        });
    }

    protected void onUndo() {

    }
}

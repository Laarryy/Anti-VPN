package me.egg82.avpn.commands;

import me.egg82.avpn.Config;
import me.egg82.avpn.VPNAPI;
import me.egg82.avpn.utils.ValidationUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.handlers.async.AsyncCommandHandler;
import ninja.egg82.utils.ThreadUtil;

public class AVPNCheckCommand extends AsyncCommandHandler {
    // vars
    private VPNAPI api = VPNAPI.getInstance();

    // constructor
    public AVPNCheckCommand() {
        super();
    }

    // public

    // private
    protected void onExecute(long elapsedMilliseconds) {
        if (!sender.hasPermission("avpn.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return;
        }
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Incorrect command usage!");
            String name = getClass().getSimpleName();
            name = name.substring(0, name.length() - 7).toLowerCase();
            ServiceLocator.getService(Plugin.class).getProxy().getPluginManager().dispatchCommand((CommandSender) sender.getHandle(), "? " + name);
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

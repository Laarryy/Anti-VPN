package me.egg82.antivpn.commands.internal;

import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.utils.LogUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommandSender sender;
    private final String ip;

    private final VPNAPI api = VPNAPI.getInstance();

    public CheckCommand(CommandSender sender, String ip) {
        this.sender = sender;
        this.ip = ip;
    }

    public void run() {
        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.YELLOW + "Checking " + ChatColor.WHITE + ip + ChatColor.YELLOW + ".."));

        Configuration config;
        try {
            config = ServiceLocator.get(Configuration.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.DARK_RED + "Internal error"));
            return;
        }

        if (config.getNode("kick", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
            double consensus = clamp(0.0d, 1.0d, config.getNode("kick", "algorithm", "min-consensus").getDouble(0.6d));
            sender.sendMessage(new TextComponent(LogUtil.getHeading() + (api.consensus(ip) >= consensus ? ChatColor.DARK_RED + "VPN/PRoxy detected" : ChatColor.GREEN + "No VPN/Proxy detected")));
        } else {
            sender.sendMessage(new TextComponent(LogUtil.getHeading() + (api.isVPN(ip) ? ChatColor.DARK_RED + "VPN/PRoxy detected" : ChatColor.GREEN + "No VPN/Proxy detected")));
        }
    }

    private double clamp(double min, double max, double val) { return Math.min(max, Math.max(min, val)); }
}

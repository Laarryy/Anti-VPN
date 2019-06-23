package me.egg82.antivpn.commands.internal;

import java.util.Optional;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.LogUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
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

        Optional<Configuration> config = ConfigUtil.getConfig();
        if (!config.isPresent()) {
            sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.DARK_RED + "Internal error"));
            return;
        }

        if (config.get().getNode("action", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
            double consensus = clamp(0.0d, 1.0d, config.get().getNode("action", "algorithm", "min-consensus").getDouble(0.6d));
            try {
                sender.sendMessage(new TextComponent(LogUtil.getHeading() + (api.consensus(ip) >= consensus ? ChatColor.DARK_RED + "VPN/Proxy detected" : ChatColor.GREEN + "No VPN/Proxy detected")));
            } catch (APIException ex) {
                logger.error(ex.getMessage(), ex);
                sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.DARK_RED + "Internal error"));
            }
        } else {
            try {
                sender.sendMessage(new TextComponent(LogUtil.getHeading() + (api.cascade(ip) ? ChatColor.DARK_RED + "VPN/Proxy detected" : ChatColor.GREEN + "No VPN/Proxy detected")));
            } catch (APIException ex) {
                logger.error(ex.getMessage(), ex);
                sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.DARK_RED + "Internal error"));
            }
        }
    }

    private double clamp(double min, double max, double val) { return Math.min(max, Math.max(min, val)); }
}

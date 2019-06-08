package me.egg82.antivpn.commands.internal;

import co.aikar.taskchain.TaskChain;
import java.util.Optional;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.LogUtil;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TaskChain<?> chain;
    private final CommandSender sender;
    private final String ip;

    private final VPNAPI api = VPNAPI.getInstance();

    public CheckCommand(TaskChain<?> chain, CommandSender sender, String ip) {
        this.chain = chain;
        this.sender = sender;
        this.ip = ip;
    }

    public void run() {
        sender.sendMessage(LogUtil.getHeading() + ChatColor.YELLOW + "Checking " + ChatColor.WHITE + ip + ChatColor.YELLOW + "..");

        chain
                .<String>asyncCallback((v, f) -> {
                    Optional<Configuration> config = ConfigUtil.getConfig();
                    if (!config.isPresent()) {
                        f.accept(ChatColor.DARK_RED + "Internal error");
                        return;
                    }

                    if (config.get().getNode("kick", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
                        double consensus = clamp(0.0d, 1.0d, config.get().getNode("kick", "algorithm", "min-consensus").getDouble(0.6d));
                        f.accept(api.consensus(ip) >= consensus ? ChatColor.DARK_RED + "VPN/PRoxy detected" : ChatColor.GREEN + "No VPN/Proxy detected");
                    } else {
                        f.accept(api.cascade(ip) ? ChatColor.DARK_RED + "VPN/PRoxy detected" : ChatColor.GREEN + "No VPN/Proxy detected");
                    }
                })
                .syncLast(v -> sender.sendMessage(LogUtil.getHeading() + v))
                .execute();
    }

    private double clamp(double min, double max, double val) { return Math.min(max, Math.max(min, val)); }
}

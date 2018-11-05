package me.egg82.antivpn.commands.internal;

import java.util.Map;
import java.util.Optional;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.utils.LogUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

public class TestCommand implements Runnable {
    private final CommandSender sender;
    private final String ip;

    private final VPNAPI api = VPNAPI.getInstance();

    public TestCommand(CommandSender sender, String ip) {
        this.sender = sender;
        this.ip = ip;
    }

    public void run() {
        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.YELLOW + "Testing with " + ChatColor.WHITE + ip + ChatColor.YELLOW + ", please wait.."));

        Map<String, Optional<Boolean>> results = api.testAllSources(ip);
        for (Map.Entry<String, Optional<Boolean>> kvp : results.entrySet()) {
            if (!kvp.getValue().isPresent()) {
                sender.sendMessage(new TextComponent(LogUtil.getHeading() + LogUtil.getSourceHeading(kvp.getKey()) + ChatColor.YELLOW + "Source error"));
                continue;
            }

            sender.sendMessage(new TextComponent(LogUtil.getHeading() + LogUtil.getSourceHeading(kvp.getKey()) + (kvp.getValue().get() ? ChatColor.DARK_RED + "VPN/PRoxy detected" : ChatColor.GREEN + "No VPN/Proxy detected")));
        }
        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.GREEN + "Test for " + ChatColor.YELLOW + ip + ChatColor.GREEN + " complete!"));
    }
}

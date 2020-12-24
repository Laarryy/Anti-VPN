package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import me.egg82.antivpn.config.ConfigurationFileUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.services.StorageMessagingHandler;
import net.md_5.bungee.api.plugin.Plugin;

public class ReloadCommand implements Runnable {
    private final Plugin plugin;
    private StorageMessagingHandler handler;
    private final CommandIssuer issuer;

    public ReloadCommand(Plugin plugin, StorageMessagingHandler handler, CommandIssuer issuer) {
        this.plugin = plugin;
        this.handler = handler;
        this.issuer = issuer;
    }

    public void run() {
        issuer.sendInfo(Message.RELOAD__BEGIN);
        ConfigurationFileUtil.reloadConfig(plugin, handler, handler);
        issuer.sendInfo(Message.RELOAD__END);
    }
}

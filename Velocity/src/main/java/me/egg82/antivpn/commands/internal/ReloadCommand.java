package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.config.ConfigurationFileUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.services.StorageMessagingHandler;

public class ReloadCommand implements Runnable {
    private final Object plugin;
    private final ProxyServer proxy;
    private final PluginDescription description;
    private StorageMessagingHandler handler;
    private final CommandIssuer issuer;

    public ReloadCommand(Object plugin, ProxyServer proxy, PluginDescription description, StorageMessagingHandler handler, CommandIssuer issuer) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.description = description;
        this.handler = handler;
        this.issuer = issuer;
    }

    public void run() {
        issuer.sendInfo(Message.RELOAD__BEGIN);
        ConfigurationFileUtil.reloadConfig(plugin, proxy, description, handler, handler);
        issuer.sendInfo(Message.RELOAD__END);
    }
}

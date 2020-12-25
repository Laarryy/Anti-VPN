package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChainFactory;
import me.egg82.antivpn.config.ConfigurationFileUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.services.StorageMessagingHandler;
import org.bukkit.plugin.Plugin;

public class ReloadCommand implements Runnable {
    private final Plugin plugin;
    private final TaskChainFactory taskFactory;
    private StorageMessagingHandler handler;
    private final CommandIssuer issuer;

    public ReloadCommand(Plugin plugin, TaskChainFactory taskFactory, StorageMessagingHandler handler, CommandIssuer issuer) {
        this.plugin = plugin;
        this.taskFactory = taskFactory;
        this.handler = handler;
        this.issuer = issuer;
    }

    public void run() {
        issuer.sendInfo(Message.RELOAD__BEGIN);

        taskFactory.<Void>newChain()
                .async(() -> ConfigurationFileUtil.reloadConfig(plugin, handler, handler))
                .sync(() -> issuer.sendInfo(Message.RELOAD__END))
                .execute();
    }
}

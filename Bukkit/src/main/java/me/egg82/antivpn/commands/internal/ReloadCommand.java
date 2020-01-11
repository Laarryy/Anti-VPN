package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChain;
import me.egg82.antivpn.enums.Message;
import me.egg82.antivpn.services.StorageMessagingHandler;
import me.egg82.antivpn.utils.ConfigurationFileUtil;
import org.bukkit.plugin.Plugin;

public class ReloadCommand implements Runnable {
    private final Plugin plugin;
    private final TaskChain<?> chain;
    private StorageMessagingHandler handler;
    private final CommandIssuer issuer;

    public ReloadCommand(Plugin plugin, TaskChain<?> chain, StorageMessagingHandler handler, CommandIssuer issuer) {
        this.plugin = plugin;
        this.chain = chain;
        this.handler = handler;
        this.issuer = issuer;
    }

    public void run() {
        issuer.sendInfo(Message.RELOAD__BEGIN);

        chain
                .async(() -> ConfigurationFileUtil.reloadConfig(plugin, handler, handler))
                .sync(() -> issuer.sendInfo(Message.RELOAD__END))
                .execute();
    }
}

package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChainFactory;
import me.egg82.antivpn.config.ConfigurationFileUtil;
import me.egg82.antivpn.lang.Message;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ReloadCommand implements Runnable {
    private final Plugin plugin;
    private final TaskChainFactory taskFactory;
    private final CommandIssuer issuer;

    public ReloadCommand(@NonNull Plugin plugin, @NonNull TaskChainFactory taskFactory, @NonNull CommandIssuer issuer) {
        this.plugin = plugin;
        this.taskFactory = taskFactory;
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

package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import java.io.File;
import me.egg82.antivpn.api.*;
import me.egg82.antivpn.api.event.api.GenericAPILoadedEvent;
import me.egg82.antivpn.api.event.api.GenericAPIReloadEvent;
import me.egg82.antivpn.api.model.ip.BukkitIPManager;
import me.egg82.antivpn.api.model.player.BukkitPlayerManager;
import me.egg82.antivpn.api.model.source.GenericSourceManager;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.config.ConfigurationFileUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.messaging.GenericMessagingHandler;
import me.egg82.antivpn.messaging.MessagingHandler;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.utils.ExceptionUtil;
import ninja.egg82.service.ServiceLocator;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ReloadCommand extends AbstractCommand {
    private final File dataFolder;
    private final CommandIssuer console;
    private final Plugin plugin;

    public ReloadCommand(@NonNull CommandIssuer issuer, @NonNull TaskChainFactory taskFactory, @NonNull File dataFolder, @NonNull CommandIssuer console, @NonNull Plugin plugin) {
        super(issuer, taskFactory);
        this.dataFolder = dataFolder;
        this.console = console;
        this.plugin = plugin;
    }

    public void run() {
        issuer.sendInfo(Message.RELOAD__BEGIN);

        TaskChain<Void> chain = taskFactory.newChain();
        chain.setErrorHandler((ex, task) -> ExceptionUtil.handleException(ex, logger));
        chain
            .async(() -> {
                CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
                if (cachedConfig == null) {
                    logger.error("Could not get cached config.");
                    return;
                }

                for (MessagingService service : cachedConfig.getMessaging()) {
                    service.close();
                }
                for (StorageService service : cachedConfig.getStorage()) {
                    service.close();
                }

                GenericSourceManager sourceManager = new GenericSourceManager();

                MessagingHandler messagingHandler = new GenericMessagingHandler();
                ServiceLocator.register(messagingHandler);

                ConfigurationFileUtil.reloadConfig(dataFolder, console, messagingHandler, sourceManager);

                cachedConfig = ConfigUtil.getCachedConfig();

                BukkitIPManager ipManager = new BukkitIPManager(plugin, sourceManager, cachedConfig.getCacheTime().getTime(), cachedConfig.getCacheTime().getUnit());
                BukkitPlayerManager playerManager = new BukkitPlayerManager(plugin, cachedConfig.getThreads(), cachedConfig.getMcLeaksKey(), cachedConfig.getCacheTime().getTime(), cachedConfig.getCacheTime().getUnit());
                VPNAPI api = VPNAPIProvider.getInstance();
                api.getEventBus().post(new GenericAPIReloadEvent(api, ipManager, playerManager, sourceManager)).now();
                api = new GenericVPNAPI(api.getPlatform(), api.getPluginMetadata(), ipManager, playerManager, sourceManager, cachedConfig, api.getEventBus());

                APIUtil.setManagers(ipManager, playerManager, sourceManager);

                APIRegistrationUtil.register(api);

                api.getEventBus().post(new GenericAPILoadedEvent(api)).now();
            })
            .syncLast(v -> issuer.sendInfo(Message.RELOAD__END))
            .execute();
    }
}

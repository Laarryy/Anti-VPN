package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import java.io.File;
import me.egg82.antivpn.api.*;
import me.egg82.antivpn.api.event.api.GenericAPILoadedEvent;
import me.egg82.antivpn.api.event.api.GenericAPIReloadEvent;
import me.egg82.antivpn.api.model.ip.BungeeIPManager;
import me.egg82.antivpn.api.model.player.BungeePlayerManager;
import me.egg82.antivpn.api.model.source.GenericSourceManager;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.config.ConfigurationFileUtil;
import me.egg82.antivpn.lang.I18NManager;
import me.egg82.antivpn.lang.MessageKey;
import me.egg82.antivpn.messaging.GenericMessagingHandler;
import me.egg82.antivpn.messaging.MessagingHandler;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.storage.StorageService;
import ninja.egg82.service.ServiceLocator;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand extends AbstractCommand {
    private final File dataFolder;
    private final CommandIssuer console;

    public ReloadCommand(@NotNull CommandIssuer issuer, @NotNull File dataFolder, @NotNull CommandIssuer console) {
        super(issuer);
        this.dataFolder = dataFolder;
        this.console = console;
    }

    public void run() {
        issuer.sendInfo(MessageKey.RELOAD__BEGIN);

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

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
        I18NManager.clearCaches();

        BungeeIPManager ipManager = new BungeeIPManager(sourceManager, cachedConfig.getCacheTime().getTime(), cachedConfig.getCacheTime().getUnit());
        BungeePlayerManager playerManager = new BungeePlayerManager(cachedConfig.getThreads(), cachedConfig.getMcLeaksKey(), cachedConfig.getCacheTime().getTime(), cachedConfig.getCacheTime().getUnit());
        VPNAPI api = VPNAPIProvider.getInstance();
        api.getEventBus().post(new GenericAPIReloadEvent(api, ipManager, playerManager, sourceManager)).now();
        api = new GenericVPNAPI(api.getPlatform(), api.getPluginMetadata(), ipManager, playerManager, sourceManager, cachedConfig, api.getEventBus());

        APIUtil.setManagers(ipManager, playerManager, sourceManager);

        APIRegistrationUtil.register(api);

        api.getEventBus().post(new GenericAPILoadedEvent(api)).now();

        issuer.sendInfo(MessageKey.RELOAD__END);
    }
}

package me.egg82.antivpn.commands.internal;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
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
import me.egg82.antivpn.lang.BukkitLocaleCommandUtil;
import me.egg82.antivpn.lang.BukkitLocalizedCommandSender;
import me.egg82.antivpn.lang.I18NManager;
import me.egg82.antivpn.lang.MessageKey;
import me.egg82.antivpn.messaging.GenericMessagingHandler;
import me.egg82.antivpn.messaging.MessagingHandler;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.storage.StorageService;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand extends AbstractCommand {
    private final File dataFolder;
    private final Plugin plugin;

    public ReloadCommand(@NotNull PaperCommandManager<BukkitLocalizedCommandSender> commandManager, @NotNull File dataFolder, @NotNull Plugin plugin) {
        super(commandManager);
        this.dataFolder = dataFolder;
        this.plugin = plugin;
    }

    public void execute(@NotNull CommandContext<BukkitLocalizedCommandSender> context) {
        commandManager.taskRecipe().begin(context)
            .asynchronous(c -> {
                c.getSender().sendMessage(MessageKey.COMMAND__RELOAD__BEGIN);

                CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
                for (MessagingService service : cachedConfig.getMessaging()) {
                    service.close();
                }
                for (StorageService service : cachedConfig.getStorage()) {
                    service.close();
                }

                GenericSourceManager sourceManager = new GenericSourceManager();
                MessagingHandler messagingHandler = new GenericMessagingHandler();
                ConfigurationFileUtil.reloadConfig(dataFolder, BukkitLocaleCommandUtil.getConsole(), messagingHandler, sourceManager);
                I18NManager.clearCaches();

                cachedConfig = ConfigUtil.getCachedConfig();
                BukkitLocaleCommandUtil.setConsoleLocale(plugin, cachedConfig.getLanguage());

                BukkitIPManager ipManager = new BukkitIPManager(plugin, sourceManager, cachedConfig.getCacheTime());
                BukkitPlayerManager playerManager = new BukkitPlayerManager(plugin, cachedConfig.getMcLeaksKey(), cachedConfig.getCacheTime());
                VPNAPI api = VPNAPIProvider.getInstance();
                api.getEventBus().post(new GenericAPIReloadEvent(api, ipManager, playerManager, sourceManager)).now();
                api = new GenericVPNAPI(api.getPlatform(), api.getPluginMetadata(), ipManager, playerManager, sourceManager, cachedConfig, api.getEventBus());

                APIUtil.setManagers(ipManager, playerManager, sourceManager);
                APIRegistrationUtil.register(api);
                api.getEventBus().post(new GenericAPILoadedEvent(api)).now();

                c.getSender().sendMessage(MessageKey.COMMAND__RELOAD__END);
            })
            .execute();
    }
}

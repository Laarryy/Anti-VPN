package me.egg82.antivpn.commands.internal;

import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.context.CommandContext;
import me.egg82.antivpn.api.APIRegistrationUtil;
import me.egg82.antivpn.api.VPNAPIImpl;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.event.api.APILoadedEventImpl;
import me.egg82.antivpn.api.event.api.APIReloadEventImpl;
import me.egg82.antivpn.api.model.ip.BukkitIPManager;
import me.egg82.antivpn.api.model.player.BukkitPlayerManager;
import me.egg82.antivpn.api.model.source.SourceManagerImpl;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.config.ConfigurationFileUtil;
import me.egg82.antivpn.locale.*;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.messaging.handler.MessagingHandler;
import me.egg82.antivpn.messaging.handler.MessagingHandlerImpl;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.utils.EventUtil;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ReloadCommand extends AbstractCommand {
    private final File dataFolder;
    private final Plugin plugin;

    public ReloadCommand(@NotNull BukkitCommandManager<BukkitLocalizedCommandSender> commandManager, @NotNull File dataFolder, @NotNull Plugin plugin) {
        super(commandManager);
        this.dataFolder = dataFolder;
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull CommandContext<BukkitLocalizedCommandSender> commandContext) {
        commandManager.taskRecipe().begin(commandContext)
                .asynchronous(c -> {
                    c.getSender().sendMessage(MessageKey.COMMAND__RELOAD__BEGIN);

                    CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
                    for (MessagingService service : cachedConfig.getMessaging()) {
                        service.close();
                    }
                    for (StorageService service : cachedConfig.getStorage()) {
                        service.close();
                    }

                    SourceManagerImpl sourceManager = new SourceManagerImpl();
                    MessagingHandler messagingHandler = new MessagingHandlerImpl();
                    ConfigurationFileUtil.reloadConfig(dataFolder, BukkitLocaleCommandUtil.getConsole(), messagingHandler, sourceManager);
                    I18NManager.clearCaches();

                    cachedConfig = ConfigUtil.getCachedConfig();
                    BukkitLocaleCommandUtil.setConsoleLocale(plugin, LocaleUtil.getDefaultI18N());

                    BukkitIPManager ipManager = new BukkitIPManager(plugin, sourceManager, cachedConfig.getCacheTime());
                    BukkitPlayerManager playerManager = new BukkitPlayerManager(plugin, cachedConfig.getMcLeaksKey(), cachedConfig.getCacheTime());
                    VPNAPIImpl api = (VPNAPIImpl) VPNAPIProvider.getInstance();
                    EventUtil.post(new APIReloadEventImpl(api, ipManager, playerManager, sourceManager), api.getEventBus());
                    api = new VPNAPIImpl(api.getPlatform(), api.getPluginMetadata(), ipManager, playerManager, sourceManager, api.getEventBus());

                    APIRegistrationUtil.register(api);
                    EventUtil.post(new APILoadedEventImpl(api), api.getEventBus());

                    c.getSender().sendMessage(MessageKey.COMMAND__RELOAD__END);
                })
                .execute();
    }
}

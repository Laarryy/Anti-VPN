package me.egg82.antivpn.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.event.VPNEvent;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.api.platform.Platform;
import me.egg82.antivpn.api.platform.PluginMetadata;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.utils.PacketUtil;
import net.engio.mbassy.bus.MBassador;
import org.jetbrains.annotations.NotNull;

public class GenericVPNAPI implements VPNAPI {
    private final Platform platform;
    private final PluginMetadata pluginMetadata;

    private final IPManager ipManager;
    private final PlayerManager playerManager;
    private final SourceManager sourceManager;
    private final MBassador<VPNEvent> eventBus;

    private final CachedConfig cachedConfig;

    public GenericVPNAPI(@NotNull Platform platform, @NotNull PluginMetadata pluginMetadata, @NotNull IPManager ipManager, @NotNull PlayerManager playerManager, @NotNull SourceManager sourceManager, @NotNull CachedConfig cachedConfig, @NotNull MBassador<VPNEvent> eventBus) {
        this.platform = platform;
        this.pluginMetadata = pluginMetadata;

        this.ipManager = ipManager;
        this.playerManager = playerManager;
        this.sourceManager = sourceManager;
        this.eventBus = eventBus;

        this.cachedConfig = cachedConfig;
    }

    public @NotNull UUID getServerId() { return cachedConfig.getServerId(); }

    public @NotNull IPManager getIPManager() { return ipManager; }

    public @NotNull PlayerManager getPlayerManager() { return playerManager; }

    public @NotNull SourceManager getSourceManager() { return sourceManager; }

    public @NotNull Platform getPlatform() { return platform; }

    public @NotNull PluginMetadata getPluginMetadata() { return pluginMetadata; }

    public @NotNull CompletableFuture<Void> runUpdateTask() { return CompletableFuture.runAsync(PacketUtil::trySendQueue); }

    public @NotNull MBassador<VPNEvent> getEventBus() { return eventBus; }
}

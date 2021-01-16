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
import org.checkerframework.checker.nullness.qual.NonNull;

public class GenericVPNAPI implements VPNAPI {
    private final Platform platform;
    private final PluginMetadata pluginMetadata;

    private final IPManager ipManager;
    private final PlayerManager playerManager;
    private final SourceManager sourceManager;
    private final MBassador<VPNEvent> eventBus;

    private final CachedConfig cachedConfig;

    public GenericVPNAPI(@NonNull Platform platform, @NonNull PluginMetadata pluginMetadata, @NonNull IPManager ipManager, @NonNull PlayerManager playerManager, @NonNull SourceManager sourceManager, @NonNull CachedConfig cachedConfig, @NonNull MBassador<VPNEvent> eventBus) {
        this.platform = platform;
        this.pluginMetadata = pluginMetadata;

        this.ipManager = ipManager;
        this.playerManager = playerManager;
        this.sourceManager = sourceManager;
        this.eventBus = eventBus;

        this.cachedConfig = cachedConfig;
    }

    public @NonNull UUID getServerId() { return cachedConfig.getServerId(); }

    public @NonNull IPManager getIPManager() { return ipManager; }

    public @NonNull PlayerManager getPlayerManager() { return playerManager; }

    public @NonNull SourceManager getSourceManager() { return sourceManager; }

    public @NonNull Platform getPlatform() { return platform; }

    public @NonNull PluginMetadata getPluginMetadata() { return pluginMetadata; }

    public @NonNull CompletableFuture<Void> runUpdateTask() { return CompletableFuture.runAsync(PacketUtil::trySendQueue); }

    public @NonNull MBassador<VPNEvent> getEventBus() { return eventBus; }
}

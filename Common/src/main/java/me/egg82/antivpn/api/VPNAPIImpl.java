package me.egg82.antivpn.api;

import me.egg82.antivpn.api.event.VPNEvent;
import me.egg82.antivpn.api.model.ip.AbstractIPManager;
import me.egg82.antivpn.api.model.player.AbstractPlayerManager;
import me.egg82.antivpn.api.model.source.SourceManagerImpl;
import me.egg82.antivpn.api.platform.Platform;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.PacketUtil;
import me.egg82.avpn.api.platform.AbstractPluginMetadata;
import net.kyori.event.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VPNAPIImpl implements VPNAPI {
    private final Platform platform;
    private final AbstractPluginMetadata pluginMetadata;

    private final AbstractIPManager ipManager;
    private final AbstractPlayerManager playerManager;
    private final SourceManagerImpl sourceManager;
    private final EventBus<VPNEvent> eventBus;

    private static VPNAPIImpl instance = null;

    public static @Nullable VPNAPIImpl get() { return instance; }

    public VPNAPIImpl(
            @NotNull Platform platform,
            @NotNull AbstractPluginMetadata pluginMetadata,
            @NotNull AbstractIPManager ipManager,
            @NotNull AbstractPlayerManager playerManager,
            @NotNull SourceManagerImpl sourceManager,
            @NotNull EventBus<VPNEvent> eventBus
    ) {
        this.platform = platform;
        this.pluginMetadata = pluginMetadata;

        this.ipManager = ipManager;
        this.playerManager = playerManager;
        this.sourceManager = sourceManager;
        this.eventBus = eventBus;

        instance = this;
    }

    @Override
    public @NotNull UUID getServerId() { return ConfigUtil.getCachedConfig().getServerId(); }

    @Override
    public @NotNull AbstractIPManager getIPManager() { return ipManager; }

    @Override
    public @NotNull AbstractPlayerManager getPlayerManager() { return playerManager; }

    @Override
    public @NotNull SourceManagerImpl getSourceManager() { return sourceManager; }

    @Override
    public @NotNull Platform getPlatform() { return platform; }

    @Override
    public @NotNull AbstractPluginMetadata getPluginMetadata() { return pluginMetadata; }

    @Override
    public @NotNull CompletableFuture<Void> runUpdateTask() { return CompletableFuture.runAsync(PacketUtil::trySendQueue); }

    @Override
    public @NotNull EventBus<VPNEvent> getEventBus() { return eventBus; }
}

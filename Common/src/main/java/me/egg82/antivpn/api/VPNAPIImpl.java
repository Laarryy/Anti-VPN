package me.egg82.antivpn.api;

import me.egg82.antivpn.api.event.VPNEvent;
import me.egg82.antivpn.api.model.ip.AbstractIPManager;
import me.egg82.antivpn.api.model.player.AbstractPlayerManager;
import me.egg82.antivpn.api.model.source.SourceManagerImpl;
import me.egg82.antivpn.api.platform.Platform;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.PacketUtil;
import me.egg82.antivpn.api.platform.AbstractPluginMetadata;
import net.kyori.event.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VPNAPIImpl implements VPNAPI {
    private final @NotNull Platform platform;
    private final @NotNull AbstractPluginMetadata pluginMetadata;

    private final @NotNull AbstractIPManager ipManager;
    private final @NotNull AbstractPlayerManager playerManager;
    private final @NotNull SourceManagerImpl sourceManager;
    private final @NotNull EventBus<VPNEvent> eventBus;

    private static @Nullable VPNAPIImpl instance = null;

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
    @NotNull
    public UUID getServerId() { return ConfigUtil.getCachedConfig().getServerId(); }

    @Override
    @NotNull
    public AbstractIPManager getIPManager() { return ipManager; }

    @Override
    @NotNull
    public AbstractPlayerManager getPlayerManager() { return playerManager; }

    @Override
    @NotNull
    public SourceManagerImpl getSourceManager() { return sourceManager; }

    @Override
    @NotNull
    public Platform getPlatform() { return platform; }

    @Override
    @NotNull
    public AbstractPluginMetadata getPluginMetadata() { return pluginMetadata; }

    @Override
    @NotNull
    public CompletableFuture<Void> runUpdateTask() { return CompletableFuture.runAsync(PacketUtil::trySendQueue); }

    @Override
    @NotNull
    public EventBus<VPNEvent> getEventBus() { return eventBus; }
}

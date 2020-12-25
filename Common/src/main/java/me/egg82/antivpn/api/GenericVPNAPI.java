package me.egg82.antivpn.api;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.api.platform.Platform;
import me.egg82.antivpn.api.platform.PluginMetadata;
import me.egg82.antivpn.messaging.ServerIDUtil;
import me.egg82.antivpn.utils.PacketUtil;
import org.checkerframework.checker.nullness.qual.NonNull;

public class GenericVPNAPI implements VPNAPI {
    private final Platform platform;
    private final PluginMetadata pluginMetadata;

    private final IPManager ipManager;
    private final PlayerManager playerManager;
    private final SourceManager sourceManager;

    private final File serverIdFile;

    public GenericVPNAPI(@NonNull Platform platform, @NonNull PluginMetadata pluginMetadata, @NonNull IPManager ipManager, @NonNull PlayerManager playerManager, @NonNull SourceManager sourceManager, @NonNull File serverIdFile) {
        this.platform = platform;
        this.pluginMetadata = pluginMetadata;

        this.ipManager = ipManager;
        this.playerManager = playerManager;
        this.sourceManager = sourceManager;

        this.serverIdFile = serverIdFile;
    }

    public @NonNull UUID getServerId() { return ServerIDUtil.getId(serverIdFile); }

    public @NonNull IPManager getIpManager() { return ipManager; }

    public @NonNull PlayerManager getPlayerManager() { return playerManager; }

    public @NonNull SourceManager getSourceManager() { return sourceManager; }

    public @NonNull Platform getPlatform() { return platform; }

    public @NonNull PluginMetadata getPluginMetadata() { return pluginMetadata; }

    public @NonNull CompletableFuture<Void> runUpdateTask() { return CompletableFuture.runAsync(PacketUtil::trySendQueue); }
}

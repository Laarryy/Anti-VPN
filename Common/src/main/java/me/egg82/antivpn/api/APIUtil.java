package me.egg82.antivpn.api;

import me.egg82.antivpn.api.model.ip.AbstractIPManager;
import me.egg82.antivpn.api.model.player.AbstractPlayerManager;
import me.egg82.antivpn.api.model.source.GenericSourceManager;
import org.jetbrains.annotations.Nullable;

public class APIUtil {
    private static AbstractIPManager ipManager = null;
    private static AbstractPlayerManager playerManager = null;
    private static GenericSourceManager sourceManager = null;

    private APIUtil() { }

    public static void setManagers(@Nullable AbstractIPManager ipManager, @Nullable AbstractPlayerManager playerManager, @Nullable GenericSourceManager sourceManager) {
        APIUtil.ipManager = ipManager;
        APIUtil.playerManager = playerManager;
        APIUtil.sourceManager = sourceManager;
    }

    public static @Nullable AbstractIPManager getIpManager() { return ipManager; }

    public static @Nullable AbstractPlayerManager getPlayerManager() { return playerManager; }

    public static @Nullable GenericSourceManager getSourceManager() { return sourceManager; }
}

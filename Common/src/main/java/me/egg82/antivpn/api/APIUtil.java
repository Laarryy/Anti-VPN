package me.egg82.antivpn.api;

import me.egg82.antivpn.api.model.ip.GenericIPManager;
import me.egg82.antivpn.api.model.player.AbstractPlayerManager;
import me.egg82.antivpn.api.model.source.GenericSourceManager;
import org.checkerframework.checker.nullness.qual.Nullable;

public class APIUtil {
    private static GenericIPManager ipManager = null;
    private static AbstractPlayerManager playerManager = null;
    private static GenericSourceManager sourceManager = null;

    private APIUtil() { }

    public static void setManagers(GenericIPManager ipManager, AbstractPlayerManager playerManager, GenericSourceManager sourceManager) {
        APIUtil.ipManager = ipManager;
        APIUtil.playerManager = playerManager;
        APIUtil.sourceManager = sourceManager;
    }

    public static @Nullable GenericIPManager getIpManager() { return ipManager; }

    public static @Nullable AbstractPlayerManager getPlayerManager() { return playerManager; }

    public static @Nullable GenericSourceManager getSourceManager() { return sourceManager; }
}

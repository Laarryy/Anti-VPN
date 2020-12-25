package me.egg82.antivpn.config;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

public class ConfigUtil {
    private static ConfigurationNode config = null;
    private static CachedConfig cachedConfig = null;

    private ConfigUtil() { }

    public static void setConfiguration(ConfigurationNode config, CachedConfig cachedConfig) {
        ConfigUtil.config = config;
        ConfigUtil.cachedConfig = cachedConfig;
    }

    public static @Nullable ConfigurationNode getConfig() { return config; }

    public static @Nullable CachedConfig getCachedConfig() { return cachedConfig; }

    public static boolean getDebugOrFalse() {
        CachedConfig c = cachedConfig; // Thread-safe reference
        return c != null && c.getDebug();
    }
}

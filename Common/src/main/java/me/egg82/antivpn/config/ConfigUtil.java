package me.egg82.antivpn.config;

import javax.annotation.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

public class ConfigUtil {
    private static ConfigurationNode config = null;
    private static CachedConfig cachedConfig = null;

    private ConfigUtil() {}

    public static void setConfiguration(ConfigurationNode config, CachedConfig cachedConfig) {
        ConfigUtil.config = config;
        ConfigUtil.cachedConfig = cachedConfig;
    }

    /**
     * Grabs the config instance from ServiceLocator
     * @return Instance of the Configuration class
     */
    @Nullable
    public static ConfigurationNode getConfig() { return config; }

    /**
     * Grabs the cached config instance from ServiceLocator
     * @return Instance of the CachedConfigValues class
     */
    @Nullable
    public static CachedConfig getCachedConfig() { return cachedConfig; }

    public static boolean getDebugOrFalse() {
        CachedConfig c = cachedConfig; // Thread-safe reference
        return c != null && c.getDebug();
    }
}

package me.egg82.antivpn.config;

import me.egg82.antivpn.lang.I18NManager;
import me.egg82.antivpn.lang.Locales;
import me.egg82.antivpn.lang.MessageKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

public class ConfigUtil {
    private static ConfigurationNode config = null;
    private static CachedConfig cachedConfig = null;

    private static I18NManager consoleLocalizationManager = null;

    private ConfigUtil() { }

    public static void setConfiguration(@Nullable ConfigurationNode config, @Nullable CachedConfig cachedConfig, @Nullable I18NManager consoleLocalizationManager) {
        ConfigUtil.config = config;
        ConfigUtil.cachedConfig = cachedConfig;
        ConfigUtil.consoleLocalizationManager = consoleLocalizationManager;
    }

    public static @NotNull ConfigurationNode getConfig() {
        if (cachedConfig == null) {
            throw new IllegalStateException(consoleLocalizationManager != null ? consoleLocalizationManager.getText(MessageKey.ERROR__NO_CONFIG) : Locales.getUS().getText(MessageKey.ERROR__NO_CONFIG));
        }
        return config;
    }

    public static @NotNull CachedConfig getCachedConfig() {
        if (cachedConfig == null) {
            throw new IllegalStateException(consoleLocalizationManager != null ? consoleLocalizationManager.getText(MessageKey.ERROR__NO_CACHED_CONFIG) : Locales.getUS().getText(MessageKey.ERROR__NO_CACHED_CONFIG));
        }
        return cachedConfig;
    }

    public static boolean getDebugOrFalse() {
        CachedConfig c = cachedConfig; // Thread-safe reference
        return c != null && c.getDebug();
    }
}

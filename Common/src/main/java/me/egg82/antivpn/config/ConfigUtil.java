package me.egg82.antivpn.config;

import me.egg82.antivpn.locale.I18NManager;
import me.egg82.antivpn.locale.LocaleUtil;
import me.egg82.antivpn.locale.MessageKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

public class ConfigUtil {
    private static ConfigurationNode config = null;
    private static CachedConfig cachedConfig = null;

    private ConfigUtil() { }

    public static void setConfiguration(@Nullable ConfigurationNode config, @Nullable CachedConfig cachedConfig) {
        ConfigUtil.config = config;
        ConfigUtil.cachedConfig = cachedConfig;
    }

    public static @NotNull ConfigurationNode getConfig() {
        ConfigurationNode c = config; // Thread-safe reference
        if (c == null) {
            I18NManager defaultI18N = LocaleUtil.getDefaultI18N();
            throw new IllegalStateException(defaultI18N.getText(MessageKey.ERROR__NO_CONFIG));
        }
        return c;
    }

    public static @NotNull CachedConfig getCachedConfig() {
        CachedConfig c = cachedConfig; // Thread-safe reference
        if (c == null) {
            I18NManager defaultI18N = LocaleUtil.getDefaultI18N();
            throw new IllegalStateException(defaultI18N.getText(MessageKey.ERROR__NO_CACHED_CONFIG));
        }
        return c;
    }

    public static boolean getDebugOrFalse() {
        CachedConfig c = cachedConfig; // Thread-safe reference
        return c != null && c.getDebug();
    }
}

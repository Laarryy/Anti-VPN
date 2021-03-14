package me.egg82.antivpn.locale;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import me.egg82.antivpn.logging.GELFLogger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class I18NManager {
    private static final Logger logger = new GELFLogger(LoggerFactory.getLogger(I18NManager.class));

    private final ResourceBundle localeBundle;
    private final Locale locale;
    private final String localeName;

    private static final ConcurrentMap<UUID, I18NManager> userCache = new ConcurrentHashMap<>();

    public static ConcurrentMap<UUID, I18NManager> getUserCache() {
        return userCache;
    }

    private static final ConcurrentMap<Locale, ResourceBundle> localeCache = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Locale, I18NManager> managerCache = new ConcurrentHashMap<>();

    public static @NotNull I18NManager getManager(@NotNull File dataDirectory, @NotNull Locale locale) {
        ResourceBundle localeBundle = localeCache.computeIfAbsent(locale, k -> {
            try {
                ResourceBundle bundle = LanguageFileUtil.getLanguage(dataDirectory, k);
                return bundle != null ? bundle : ResourceBundle.getBundle("", locale);
            } catch (IOException ex) {
                logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
            return ResourceBundle.getBundle("", locale);
        });
        return managerCache.computeIfAbsent(locale, k -> new I18NManager(localeBundle));
    }

    public static void clearCaches() {
        localeCache.clear();
        managerCache.clear();
        userCache.clear();
    }

    public I18NManager(@NotNull ResourceBundle localeBundle) {
        this.localeBundle = localeBundle;
        this.locale = this.localeBundle.getLocale();
        this.localeName = getLocaleName(this.locale);
    }

    public @NotNull String getText(@NotNull MessageKey key) {
        try {
            return localeBundle.getString(key.getKey());
        } catch (MissingResourceException ex) {
            I18NManager defaultI18N = LocaleUtil.getDefaultI18N();
            if (!locale.equals(defaultI18N.locale)) {
                logger.error(getText(MessageKey.ERROR__LANG__NO_LOCALE_TEXT, "{lang}", localeName, "{key}", key.getKey()));
                return defaultI18N.getText(key);
            }
        }
        throw new IllegalStateException(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__LANG__NO_LOCALE_TEXT, "{lang}", localeName, "{key}", key.getKey()));
    }

    public @NotNull String getText(@NotNull MessageKey key, String... placeholders) {
        if (placeholders == null || placeholders.length == 0) {
            return getText(key);
        }

        String message = getText(key);
        String placeholder = null;
        for (String p : placeholders) {
            if (placeholder == null) {
                placeholder = p;
            } else {
                message = message.replace(placeholder, p);
                placeholder = null;
            }
        }

        return message;
    }

    public @NotNull String getText(@NotNull MessageKey key, @NotNull Map<String, String> placeholders) {
        if (placeholders.isEmpty()) {
            return getText(key);
        }

        String message = getText(key);
        for (Map.Entry<String, String> kvp : placeholders.entrySet()) {
            message = message.replace(kvp.getKey(), kvp.getValue());
        }
        return message;
    }

    public @NotNull Locale getLocale() {
        return locale;
    }

    public @NotNull String getLocaleName() {
        return localeName;
    }

    private static @NotNull String getLocaleName(@NotNull Locale locale) {
        return locale.getCountry() == null || locale.getCountry().isEmpty() ? locale.getLanguage() : locale.getLanguage() + "_" + locale.getCountry();
    }
}

package me.egg82.antivpn.lang;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import me.egg82.antivpn.core.Pair;
import me.egg82.antivpn.logging.GELFLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class I18NManager {
    private static final Logger logger = LoggerFactory.getLogger(I18NManager.class);

    private final ResourceBundle localeBundle;
    private final Locale locale;
    private final String localeName;

    private final ResourceBundle backupBundle;
    private final Locale backupLocale;
    private final String backupLocaleName;

    private boolean bundlesAreEqual = false;

    private static final ConcurrentMap<UUID, I18NManager> userCache = new ConcurrentHashMap<>();
    public static ConcurrentMap<UUID, I18NManager> getUserCache() { return userCache; }

    private static final ConcurrentMap<Locale, ResourceBundle> localeCache = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Pair<Locale, Locale>, I18NManager> managerCache = new ConcurrentHashMap<>();

    public static @NotNull I18NManager getManager(@NotNull File dataDirectory, @Nullable Locale locale, @NotNull Locale backupLocale) throws IOException {
        ResourceBundle localeBundle = locale == null ? null : localeCache.computeIfAbsent(locale, k -> {
            try {
                return LanguageFileUtil.getLanguage(dataDirectory, k);
            } catch (IOException ex) {
                GELFLogger.exception(logger, ex);
            }
            return null;
        });

        ResourceBundle backupBundle = localeCache.computeIfAbsent(backupLocale, k -> {
            try {
                return LanguageFileUtil.getLanguage(dataDirectory, k);
            } catch (IOException ex) {
                GELFLogger.exception(logger, ex);
            }
            return null;
        });

        if (backupBundle == null) {
            throw new IOException(Locales.getUS().getText(MessageKey.ERROR__LANG__NO_BACKUP_BUNDLE, "{lang}", getLocaleName(backupLocale)));
        }

        return managerCache.computeIfAbsent(new Pair<>(locale, backupLocale), k -> new I18NManager(localeBundle, backupBundle));
    }

    public static void clearCaches() {
        localeCache.clear();
        managerCache.clear();
        userCache.clear();
    }

    public I18NManager(@Nullable ResourceBundle localeBundle, @NotNull ResourceBundle backupBundle) {
        if (localeBundle == null || localeBundle.getLocale().equals(backupBundle.getLocale())) {
            bundlesAreEqual = true;
            this.localeBundle = backupBundle;
        } else {
            this.localeBundle = localeBundle;
        }
        this.locale = this.localeBundle.getLocale();
        this.localeName = getLocaleName(this.locale);
        this.backupBundle = backupBundle;
        this.backupLocale = this.backupBundle.getLocale();
        this.backupLocaleName = getLocaleName(this.backupLocale);
    }

    public @NotNull String getText(@NotNull MessageKey key) {
        try {
            return localeBundle.getString(key.getKey());
        } catch (MissingResourceException ex) {
            if (bundlesAreEqual) {
                GELFLogger.error(logger, this, MessageKey.ERROR__LANG__NO_BACKUP_TEXT, "{lang}", localeName, "{key}", key.getKey());
            } else {
                GELFLogger.error(logger, this, MessageKey.ERROR__LANG__NO_LOCALE_TEXT, "{lang}", localeName, "{key}", key.getKey());
                try {
                    return backupBundle.getString(key.getKey());
                } catch (MissingResourceException ex2) {
                    GELFLogger.error(logger, this, MessageKey.ERROR__LANG__NO_BACKUP_TEXT, "{lang}", localeName, "{key}", key.getKey());
                    if (!backupBundle.getLocale().equals(Locales.getUSLocale())) {
                        try {
                            return Locales.getUS().getText(key);
                        } catch (MissingResourceException | IllegalStateException ex3) {
                            GELFLogger.exception(logger, ex3);
                        }
                    }
                }
            }
        }
        throw new IllegalStateException(Locales.getUS().getText(MessageKey.ERROR__LANG__NO_LOCALE_TEXT, "{lang}", localeName, "{key}", key.getKey()));
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

    public @NotNull Locale getLocale() { return locale; }

    public @NotNull Locale getBackupLocale() { return backupLocale; }

    public @NotNull String getLocaleName() { return localeName; }

    public @NotNull String getBackupLocaleName() { return backupLocaleName; }

    public boolean isBackupLocale() { return bundlesAreEqual; }

    private static @NotNull String getLocaleName(@NotNull Locale locale) { return locale.getCountry() == null || locale.getCountry().isEmpty() ? locale.getLanguage() : locale.getLanguage() + "_" + locale.getCountry(); }
}

package me.egg82.antivpn.lang;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;
import me.egg82.antivpn.logging.GELFLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Locales {
    private static final Logger logger = LoggerFactory.getLogger(Locales.class);

    private Locales() { }

    public static @NotNull Locale getUSLocale() { return Locale.US; }

    private static final ResourceBundle unitedStatesBundle = getInternalBundle("/lang/lang_en_US.properties", Locale.US);
    public static @NotNull ResourceBundle getUSBundle() { return unitedStatesBundle; }

    private static final I18NManager unitedStatesManager = new I18NManager(unitedStatesBundle, unitedStatesBundle);
    public static @NotNull I18NManager getUS() { return unitedStatesManager; }

    private static @NotNull ResourceBundle getInternalBundle(@NotNull String resourcePath, @NotNull Locale locale) {
        try (InputStream inStream = LanguageFileUtil.class.getResourceAsStream(resourcePath)) {
            return new LocalePropertyResourceBundle(inStream, locale);
        } catch (IOException ex) {
            GELFLogger.exception(logger, ex);
            try {
                return new LocalePropertyResourceBundle(locale);
            } catch (IOException ex2) {
                GELFLogger.exception(logger, ex2);
                throw new IllegalStateException("LocalePropertyResourceBundle could not be created.", ex2);
            }
        }
    }

    public static @NotNull Locale parseLocale(@Nullable String search, @NotNull I18NManager consoleLocalizationManager) {
        for (Locale locale : Locale.getAvailableLocales()) {
            String l = locale.getCountry() == null || locale.getCountry().isEmpty() ? locale.getLanguage() : locale.getLanguage() + "-" + locale.getCountry();
            if (locale.getLanguage().equalsIgnoreCase(search) || l.equalsIgnoreCase(search)) {
                return locale;
            }
            if (locale.getCountry() != null && !locale.getCountry().isEmpty()) {
                l = locale.getLanguage() + "_" + locale.getCountry();
                if (locale.getLanguage().equalsIgnoreCase(search) || l.equalsIgnoreCase(search)) {
                    return locale;
                }
            }
        }
        GELFLogger.warn(logger, consoleLocalizationManager, MessageKey.ERROR__LANG__NO_LOCALE, "{lang}", search);
        return getUSLocale();
    }
}

package me.egg82.antivpn.locale;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;

import me.egg82.antivpn.logging.GELFLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocaleUtil {
    private static final Logger logger = new GELFLogger(LoggerFactory.getLogger(LocaleUtil.class));

    private static I18NManager consoleI18N = null;
    private static final I18NManager usI18N = new I18NManager(getInternalBundle("/lang/lang_en_US.properties", Locale.US));

    private LocaleUtil() { }

    public static void setLocale(@Nullable I18NManager consoleI18N) {
        LocaleUtil.consoleI18N = consoleI18N;
    }

    public static @Nullable I18NManager getConsoleI18N() { return consoleI18N; }

    public static @NotNull I18NManager getUSI18N() { return usI18N; }

    public static @NotNull I18NManager getDefaultI18N() {
        I18NManager m = consoleI18N; // Thread-safe reference
        return m != null ? m : usI18N;
    }

    public static @NotNull Locale parseLocale(@Nullable String search) {
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
        logger.warn(getDefaultI18N().getText(MessageKey.ERROR__LANG__NO_LOCALE, "{lang}", search));
        I18NManager m = consoleI18N; // Thread-safe reference
        return m != null ? m.getLocale() : Locale.US;
    }

    private static @NotNull ResourceBundle getInternalBundle(@NotNull String resourcePath, @NotNull Locale locale) {
        try (InputStream inStream = LanguageFileUtil.class.getResourceAsStream(resourcePath)) {
            return new LocalePropertyResourceBundle(inStream, locale);
        } catch (IOException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            try {
                return new LocalePropertyResourceBundle(locale);
            } catch (IOException ex2) {
                logger.error(ex2.getClass().getName() + ": " + ex2.getMessage(), ex2);
                throw new IllegalStateException("LocalePropertyResourceBundle could not be created.", ex2);
            }
        }
    }
}

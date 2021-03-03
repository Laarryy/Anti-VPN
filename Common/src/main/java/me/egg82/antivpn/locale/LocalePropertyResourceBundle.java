package me.egg82.antivpn.locale;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import org.jetbrains.annotations.NotNull;

public class LocalePropertyResourceBundle extends PropertyResourceBundle {
    private final Locale locale;

    public LocalePropertyResourceBundle(@NotNull InputStream stream, @NotNull Locale locale) throws IOException {
        super(stream);
        this.locale = locale;
    }

    public LocalePropertyResourceBundle(@NotNull Reader reader, @NotNull Locale locale) throws IOException {
        super(reader);
        this.locale = locale;
    }

    public LocalePropertyResourceBundle(@NotNull Locale locale) throws IOException {
        super(new StringReader("version = 1.0"));
        this.locale = locale;
    }

    public @NotNull Locale getLocale() { return locale; }
}

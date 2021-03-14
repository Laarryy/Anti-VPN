package me.egg82.antivpn.locale;

import cloud.commandframework.ArgumentDescription;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class LocalizedArgumentDescription implements ArgumentDescription {
    public static ArgumentDescription of(@NotNull MessageKey key) { return new LocalizedArgumentDescription(LocaleUtil.getDefaultI18N(), key); }

    public static ArgumentDescription of(@NotNull MessageKey key, String... placeholders) {
        return new LocalizedArgumentDescription(
                LocaleUtil.getDefaultI18N(),
                key,
                placeholders
        );
    }

    public static ArgumentDescription of(
            @NotNull MessageKey key,
            @NotNull Map<String, String> placeholders
    ) { return new LocalizedArgumentDescription(LocaleUtil.getDefaultI18N(), key, placeholders); }

    public static ArgumentDescription of(@NotNull I18NManager localizationManager, @NotNull MessageKey key) {
        return new LocalizedArgumentDescription(
                localizationManager,
                key
        );
    }

    public static ArgumentDescription of(
            @NotNull I18NManager localizationManager,
            @NotNull MessageKey key,
            String... placeholders
    ) { return new LocalizedArgumentDescription(localizationManager, key, placeholders); }

    public static ArgumentDescription of(
            @NotNull I18NManager localizationManager,
            @NotNull MessageKey key,
            @NotNull Map<String, String> placeholders
    ) { return new LocalizedArgumentDescription(localizationManager, key, placeholders); }

    private final String description;

    private LocalizedArgumentDescription(@NotNull I18NManager localizationManager, @NotNull MessageKey key) {
        this.description = localizationManager.getText(key);
    }

    private LocalizedArgumentDescription(@NotNull I18NManager localizationManager, @NotNull MessageKey key, String... placeholders) {
        this.description = localizationManager.getText(key, placeholders);
    }

    private LocalizedArgumentDescription(@NotNull I18NManager localizationManager, @NotNull MessageKey key, @NotNull Map<String, String> placeholders) {
        this.description = localizationManager.getText(key, placeholders);
    }

    @Override
    public @NotNull String getDescription() { return description; }

    @Override
    public boolean isEmpty() { return false; }
}

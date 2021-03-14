package me.egg82.antivpn.locale;

import java.util.Map;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface LocalizedCommandSender<M extends LocalizedCommandSender<M, B>, B> {
    @NotNull B getBase();

    @NotNull Audience getAudience();

    void sendMessage(@NotNull String message);

    void sendMessage(@NotNull String message, String... placeholders);

    void sendMessage(@NotNull String message, @NotNull Map<String, String> placeholders);

    default void sendMessage(@NotNull MessageKey key) {
        sendMessage(getLocalizedText(key));
    }

    default void sendMessage(@NotNull MessageKey key, String... placeholders) {
        sendMessage(getLocalizedText(key, placeholders));
    }

    default void sendMessage(@NotNull MessageKey key, @NotNull Map<String, String> placeholders) {
        sendMessage(getLocalizedText(key, placeholders));
    }

    default @NotNull String getLocalizedText(@NotNull MessageKey key) { return getLocalizationManager().getText(key); }

    default @NotNull String getLocalizedText(@NotNull MessageKey key, String... placeholders) { return getLocalizationManager().getText(key, placeholders); }

    default @NotNull String getLocalizedText(@NotNull MessageKey key, Map<String, String> placeholders) { return getLocalizationManager().getText(key, placeholders); }

    @NotNull Component getComponent(@NotNull String message);

    default @NotNull Component getComponent(@NotNull MessageKey key) { return getComponent(getLocalizedText(key)); }

    default @NotNull Component getComponent(@NotNull MessageKey key, String... placeholders) { return getComponent(getLocalizedText(key, placeholders)); }

    default @NotNull Component getComponent(@NotNull MessageKey key, @NotNull Map<String, String> placeholders) {
        return getComponent(getLocalizedText(
                key,
                placeholders
        ));
    }

    @NotNull I18NManager getLocalizationManager();

    boolean isConsole();

    boolean isUser();
}

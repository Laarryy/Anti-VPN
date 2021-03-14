package me.egg82.antivpn.locale;

import java.util.Map;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractLocalizedCommandSender<M extends AbstractLocalizedCommandSender<M, B>, B> implements LocalizedCommandSender<M, B> {
    protected static final MiniMessage formatter = MiniMessage.get();

    protected final B base;
    protected final Audience audience;
    protected final I18NManager localizationManager;

    protected AbstractLocalizedCommandSender(@NotNull B base, @NotNull Audience audience, @NotNull I18NManager localizationManager) {
        this.base = base;
        this.audience = audience;
        this.localizationManager = localizationManager;
    }

    @Override
    public @NotNull B getBase() { return base; }

    @Override
    public @NotNull Audience getAudience() { return audience; }

    @Override
    public void sendMessage(@NotNull String message) {
        getAudience().sendMessage(formatter.parse(localizationManager.getText(MessageKey.GENERAL__DECORATOR)).append(formatter.parse(message)));
    }

    @Override
    public void sendMessage(@NotNull String message, String... placeholders) {
        getAudience().sendMessage(formatter.parse(localizationManager.getText(MessageKey.GENERAL__DECORATOR)).append(formatter.parse(message, placeholders)));
    }

    @Override
    public void sendMessage(@NotNull String message, @NotNull Map<String, String> placeholders) {
        getAudience().sendMessage(formatter.parse(localizationManager.getText(MessageKey.GENERAL__DECORATOR)).append(formatter.parse(message, placeholders)));
    }

    @Override
    public @NotNull Component getComponent(@NotNull String message) {
        return formatter.parse(localizationManager.getText(MessageKey.GENERAL__DECORATOR))
                .append(formatter.parse(message));
    }

    @Override
    public @NotNull I18NManager getLocalizationManager() { return localizationManager; }
}

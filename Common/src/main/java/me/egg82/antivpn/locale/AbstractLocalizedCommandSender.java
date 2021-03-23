package me.egg82.antivpn.locale;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class AbstractLocalizedCommandSender<M extends AbstractLocalizedCommandSender<M, B>, B> implements LocalizedCommandSender<M, B> {
    protected static final @NotNull MiniMessage formatter = MiniMessage.get();

    protected final @NotNull B base;
    protected final @NotNull Audience audience;
    protected final @NotNull I18NManager localizationManager;

    protected AbstractLocalizedCommandSender(@NotNull B base, @NotNull Audience audience, @NotNull I18NManager localizationManager) {
        this.base = base;
        this.audience = audience;
        this.localizationManager = localizationManager;
    }

    @Override
    @NotNull
    public B getBase() { return base; }

    @Override
    @NotNull
    public Audience getAudience() { return audience; }

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
    @NotNull
    public Component getComponent(@NotNull String message) {
        return formatter.parse(localizationManager.getText(MessageKey.GENERAL__DECORATOR))
                .append(formatter.parse(message));
    }

    @Override
    @NotNull
    public I18NManager getLocalizationManager() { return localizationManager; }
}

package me.egg82.antivpn.locale;

import co.aikar.commands.CommandManager;
import co.aikar.commands.VelocityMessageFormatter;
import co.aikar.locales.MessageKeyProvider;
import org.jetbrains.annotations.NotNull;

public class PluginMessageFormatter extends VelocityMessageFormatter {
    private final String header;

    public PluginMessageFormatter(@NotNull CommandManager manager, @NotNull MessageKeyProvider header) {
        this(manager.getLocales().getMessage(null, header));
    }

    public PluginMessageFormatter(@NotNull String header) {
        super();
        this.header = header;
    }

    public @NotNull String format(@NotNull String message) {
        message = header + message;
        return super.format(message);
    }
}

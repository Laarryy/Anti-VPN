package me.egg82.antivpn.lang;

import co.aikar.commands.BukkitMessageFormatter;
import co.aikar.commands.CommandManager;
import co.aikar.locales.MessageKeyProvider;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PluginMessageFormatter extends BukkitMessageFormatter {
    private final String header;

    public PluginMessageFormatter(@NonNull CommandManager manager, @NonNull MessageKeyProvider header) { this(manager.getLocales().getMessage(null, header)); }

    public PluginMessageFormatter(@NonNull String header) {
        super();
        this.header = header;
    }

    public @NonNull String format(@NonNull String message) {
        message = header + message;
        return super.format(message);
    }
}

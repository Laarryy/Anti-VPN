package me.egg82.antivpn.locale;

import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BukkitLocalizedCommandSender extends AbstractLocalizedCommandSender<BukkitLocalizedCommandSender, CommandSender> {
    private final boolean isConsole;
    private final boolean isUser;

    public BukkitLocalizedCommandSender(@NotNull CommandSender base, @NotNull Audience audience, @NotNull I18NManager localizationManager) {
        super(base, audience, localizationManager);

        this.isConsole = base instanceof ConsoleCommandSender;
        this.isUser = base instanceof Player;
    }

    public static @NotNull BukkitLocalizedCommandSender getMappedCommandSender(
            @NotNull CommandSender base,
            @NotNull Audience audience,
            @NotNull I18NManager localizationManager
    ) { return new BukkitLocalizedCommandSender(base, audience, localizationManager); }

    public static @NotNull CommandSender getBaseCommandSender(@NotNull BukkitLocalizedCommandSender mapped) { return mapped.base; }

    public static @NotNull Audience getMappedAudience(@NotNull BukkitLocalizedCommandSender sender) { return sender.getAudience(); }

    @Override
    public boolean isConsole() { return isConsole; }

    @Override
    public boolean isUser() { return isUser; }
}

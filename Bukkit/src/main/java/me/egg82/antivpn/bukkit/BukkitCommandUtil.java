package me.egg82.antivpn.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class BukkitCommandUtil {
    private BukkitCommandUtil() { }

    public static void dispatchCommands(@NotNull Collection<String> commands, @NotNull CommandSender issuer, @NotNull Plugin plugin) {
        dispatchCommands(commands, issuer, plugin, Bukkit.isPrimaryThread());
    }

    public static void dispatchCommands(@NotNull Collection<String> commands, @NotNull CommandSender issuer, @NotNull Plugin plugin, boolean isAsync) {
        if (isAsync) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (String command : commands) {
                    Bukkit.dispatchCommand(issuer, command);
                }
            });
        } else {
            for (String command : commands) {
                Bukkit.dispatchCommand(issuer, command);
            }
        }
    }

    public static void dispatchCommand(@NotNull String command, @NotNull CommandSender issuer, @NotNull Plugin plugin) {
        dispatchCommand(command, issuer, plugin, Bukkit.isPrimaryThread());
    }

    public static void dispatchCommand(@NotNull String command, @NotNull CommandSender issuer, @NotNull Plugin plugin, boolean isAsync) {
        if (isAsync) {
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(issuer, command));
        } else {
            Bukkit.dispatchCommand(issuer, command);
        }
    }
}

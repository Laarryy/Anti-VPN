package me.egg82.antivpn.utils;

import java.util.Collection;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BukkitCommandUtil {
    private BukkitCommandUtil() { }

    public static void dispatchCommands(@NonNull Collection<String> commands, @NonNull CommandSender issuer, @NonNull Plugin plugin) { dispatchCommands(commands, issuer, plugin, Bukkit.isPrimaryThread()); }

    public static void dispatchCommands(@NonNull Collection<String> commands, @NonNull CommandSender issuer, @NonNull Plugin plugin, boolean isAsync) {
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

    public static void dispatchCommand(@NonNull String command, @NonNull CommandSender issuer, @NonNull Plugin plugin) { dispatchCommand(command, issuer, plugin, Bukkit.isPrimaryThread()); }

    public static void dispatchCommand(@NonNull String command, @NonNull CommandSender issuer, @NonNull Plugin plugin, boolean isAsync) {
        if (isAsync) {
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(issuer, command));
        } else {
            Bukkit.dispatchCommand(issuer, command);
        }
    }
}

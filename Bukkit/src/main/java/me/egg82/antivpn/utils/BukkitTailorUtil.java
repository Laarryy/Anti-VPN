package me.egg82.antivpn.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.egg82.antivpn.hooks.PlaceholderAPIHook;
import ninja.egg82.service.ServiceLocator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BukkitTailorUtil {
    private static final Logger logger = LoggerFactory.getLogger(BukkitTailorUtil.class);

    private BukkitTailorUtil() { }

    public static @NotNull List<String> tailorCommands(@NotNull List<String> commands, @NotNull String name, @NotNull UUID uuid, @NotNull String ip) {
        List<String> retVal = new ArrayList<>();

        Optional<PlaceholderAPIHook> placeholderapi;
        try {
            placeholderapi = ServiceLocator.getOptional(PlaceholderAPIHook.class);
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
            placeholderapi = Optional.empty();
        }

        for (String command : commands) {
            command = command.replace("%player%", name).replace("%uuid%", uuid.toString()).replace("%ip%", ip);
            if (command.charAt(0) == '/') {
                command = command.substring(1);
            }

            if (placeholderapi.isPresent()) {
                Player p = Bukkit.getPlayer(uuid);
                retVal.add(placeholderapi.get().withPlaceholders(p != null ? p : Bukkit.getOfflinePlayer(uuid), command));
            } else {
                retVal.add(command);
            }
        }

        return retVal;
    }

    public static @NotNull String tailorKickMessage(@NotNull String message, @NotNull String name, @NotNull UUID uuid, @NotNull String ip) {
        Optional<PlaceholderAPIHook> placeholderapi;
        try {
            placeholderapi = ServiceLocator.getOptional(PlaceholderAPIHook.class);
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
            placeholderapi = Optional.empty();
        }

        message = message.replace("%player%", name).replace("%uuid%", uuid.toString()).replace("%ip%", ip);
        if (placeholderapi.isPresent()) {
            Player p = Bukkit.getPlayer(uuid);
            message = placeholderapi.get().withPlaceholders(p != null ? p : Bukkit.getOfflinePlayer(uuid), message);
        }
        return ChatColor.translateAlternateColorCodes('&', message.replace("\\r", "").replace("\r", "").replace("\\n", "\n"));
    }
}

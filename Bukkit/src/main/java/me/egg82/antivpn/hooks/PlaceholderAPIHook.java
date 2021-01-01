package me.egg82.antivpn.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PlaceholderAPIHook implements PluginHook {
    public PlaceholderAPIHook() { }

    public void cancel() { }

    public @NonNull String withPlaceholders(Player player, @NonNull String input) { return PlaceholderAPI.setPlaceholders(player, input); }

    public @NonNull String withPlaceholders(OfflinePlayer player, @NonNull String input) { return PlaceholderAPI.setPlaceholders(player, input); }
}

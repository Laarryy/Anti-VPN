package me.egg82.antivpn.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.security.PublicKey;

public class PlaceholderAPIHook implements PluginHook {
    public PlaceholderAPIHook() {}

    public void cancel() {}

    public String withPlaceholders(Player player, String input) { return PlaceholderAPI.setPlaceholders(player, input); }

    public String withPlaceholders(OfflinePlayer player, String input) { return PlaceholderAPI.setPlaceholders(player, input); }
}

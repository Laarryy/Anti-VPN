package me.egg82.antivpn.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import ninja.egg82.events.BukkitEvents;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPIHook implements PluginHook {
    public static void create(@NotNull Plugin plugin, @NotNull Plugin placeholderapi) {
        if (!placeholderapi.isEnabled()) {
            BukkitEvents.subscribe(plugin, PluginEnableEvent.class, EventPriority.MONITOR)
                    .expireIf(e -> e.getPlugin().getName().equals("PlaceholderAPI"))
                    .filter(e -> e.getPlugin().getName().equals("PlaceholderAPI"))
                    .handler(e -> hook = new PlaceholderAPIHook());
            return;
        }
        hook = new PlaceholderAPIHook();
    }

    private static PlaceholderAPIHook hook = null;

    public static @Nullable PlaceholderAPIHook get() { return hook; }

    private PlaceholderAPIHook() {
        PluginHooks.getHooks().add(this);
    }

    @Override
    public void cancel() { }

    public @NotNull String withPlaceholders(@NotNull String input) { return PlaceholderAPI.setPlaceholders(null, input); }

    public @NotNull String withPlaceholders(@NotNull Player player, @NotNull String input) { return PlaceholderAPI.setPlaceholders(player, input); }

    public @NotNull String withPlaceholders(@NotNull OfflinePlayer player, @NotNull String input) { return PlaceholderAPI.setPlaceholders(player, input); }
}

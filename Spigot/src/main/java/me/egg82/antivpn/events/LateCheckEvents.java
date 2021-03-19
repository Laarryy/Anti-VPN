package me.egg82.antivpn.events;

import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.bukkit.BukkitCommandUtil;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.hooks.BStatsHook;
import me.egg82.antivpn.hooks.LuckPermsHook;
import me.egg82.antivpn.hooks.VaultHook;
import me.egg82.antivpn.locale.BukkitLocaleCommandUtil;
import me.egg82.antivpn.locale.MessageKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.craftbukkit.BukkitComponentSerializer;
import ninja.egg82.events.BukkitEvents;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class LateCheckEvents extends EventHolder {
    private final Plugin plugin;

    public LateCheckEvents(@NotNull Plugin plugin) {
        this.plugin = plugin;

        events.add(
                BukkitEvents.subscribe(plugin, PlayerLoginEvent.class, EventPriority.LOWEST)
                        .filter(e -> LuckPermsHook.get() == null && (VaultHook.get() == null || VaultHook.get()
                                .getPermission() == null)) // Player has already been processed
                        .filter(e -> {
                            if (Bukkit.hasWhitelist() && !e.getPlayer().isWhitelisted()) {
                                if (ConfigUtil.getDebugOrFalse()) {
                                    BukkitLocaleCommandUtil.getConsole().sendMessage(
                                            MessageKey.DEBUG__NOT_WHITELISTED,
                                            "{name}", e.getPlayer().getName(),
                                            "{uuid}", e.getPlayer().getUniqueId().toString()
                                    );
                                }
                                return false;
                            }
                            return true;
                        })
                        .handler(this::checkPerms)
                        .exceptionHandler(ex -> logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex))
        );
    }

    private void checkPerms(@NotNull PlayerLoginEvent event) {
        String ip = getIp(event.getAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        tryActOnPlayer(ip, event, event.getPlayer().hasPermission(ConfigUtil.getCachedConfig().getBypassPermissionNode()));
    }

    @SuppressWarnings("deprecation")
    private void tryActOnPlayer(@NotNull String ip, @NotNull PlayerLoginEvent event, boolean hasBypassPermission) {
        if (hasBypassPermission) {
            if (ConfigUtil.getDebugOrFalse()) {
                BukkitLocaleCommandUtil.getConsole().sendMessage(
                        MessageKey.DEBUG__BYPASS_CHECK,
                        "{name}", event.getPlayer().getName(),
                        "{uuid}", event.getPlayer().getUniqueId().toString()
                );
            }
            return;
        }

        if (isIgnoredIp(ip, event.getPlayer().getName(), event.getPlayer().getUniqueId())) {
            return;
        }

        if (isVpn(ip, event.getPlayer().getName(), event.getPlayer().getUniqueId())) {
            BStatsHook.incrementBlockedVPNs();
            IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();
            BukkitCommandUtil.dispatchCommands(
                    ipManager.getVpnCommands(event.getPlayer().getName(), event.getPlayer().getUniqueId(), ip),
                    Bukkit.getConsoleSender(),
                    plugin,
                    event.isAsynchronous()
            );
            Component kickMessage = ipManager.getVpnKickMessage(event.getPlayer().getName(), event.getPlayer().getUniqueId(), ip);
            if (kickMessage != null) {
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(BukkitComponentSerializer.legacy().serialize(kickMessage));
            }
        }

        if (isMcLeaks(event.getPlayer().getName(), event.getPlayer().getUniqueId())) {
            BStatsHook.incrementBlockedMCLeaks();
            PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();
            BukkitCommandUtil.dispatchCommands(
                    playerManager.getMcLeaksCommands(event.getPlayer().getName(), event.getPlayer().getUniqueId(), ip),
                    Bukkit.getConsoleSender(),
                    plugin,
                    event.isAsynchronous()
            );
            Component kickMessage = playerManager.getMcLeaksKickMessage(event.getPlayer().getName(), event.getPlayer().getUniqueId(), ip);
            if (kickMessage != null) {
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(BukkitComponentSerializer.legacy().serialize(kickMessage));
            }
        }
    }
}

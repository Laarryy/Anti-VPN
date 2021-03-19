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
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class EarlyCheckEvents extends EventHolder {
    private final Plugin plugin;

    public EarlyCheckEvents(@NotNull Plugin plugin) {
        this.plugin = plugin;

        events.add(
                BukkitEvents.subscribe(plugin, AsyncPlayerPreLoginEvent.class, EventPriority.LOWEST)
                        .filter(e -> {
                            if (Bukkit.hasWhitelist() && !isWhitelisted(e.getUniqueId())) {
                                if (ConfigUtil.getDebugOrFalse()) {
                                    BukkitLocaleCommandUtil.getConsole().sendMessage(
                                            MessageKey.DEBUG__NOT_WHITELISTED,
                                            "{name}", e.getName(),
                                            "{uuid}", e.getUniqueId().toString()
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

    private void checkPerms(@NotNull AsyncPlayerPreLoginEvent event) {
        String ip = getIp(event.getAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        String bypassNode = ConfigUtil.getCachedConfig().getBypassPermissionNode();

        LuckPermsHook luckPermsHook = LuckPermsHook.get();
        if (luckPermsHook != null) {
            // LuckPerms is available, check permissions + data and kick if needed
            boolean val = false;
            try {
                val = luckPermsHook.hasPermission(event.getUniqueId(), bypassNode).get();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | CancellationException ex) {
                logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
            tryActOnPlayer(ip, event, val);
        } else {
            // LuckPerms is not available, check for Vault
            VaultHook vaultHook = VaultHook.get();
            if (vaultHook != null && vaultHook.getPermission() != null) {
                // Vault is available, check permissions + data and kick if needed
                tryActOnPlayer(ip, event, vaultHook.getPermission().playerHas(null, Bukkit.getOfflinePlayer(event.getUniqueId()), bypassNode));
            } else {
                // Vault is not available, only cache data
                cacheData(ip, event.getUniqueId());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void tryActOnPlayer(@NotNull String ip, @NotNull AsyncPlayerPreLoginEvent event, boolean hasBypassPermission) {
        if (hasBypassPermission) {
            if (ConfigUtil.getDebugOrFalse()) {
                BukkitLocaleCommandUtil.getConsole().sendMessage(
                        MessageKey.DEBUG__BYPASS_CHECK,
                        "{name}", event.getName(),
                        "{uuid}", event.getUniqueId().toString()
                );
            }
            return;
        }

        if (isIgnoredIp(ip, event.getName(), event.getUniqueId())) {
            return;
        }

        if (isVpn(ip, event.getName(), event.getUniqueId())) {
            BStatsHook.incrementBlockedVPNs();
            IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();
            BukkitCommandUtil.dispatchCommands(
                    ipManager.getVpnCommands(event.getName(), event.getUniqueId(), ip),
                    Bukkit.getConsoleSender(),
                    plugin,
                    event.isAsynchronous()
            );
            Component kickMessage = ipManager.getVpnKickMessage(event.getName(), event.getUniqueId(), ip);
            if (kickMessage != null) {
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(BukkitComponentSerializer.legacy().serialize(kickMessage));
            }
        }

        if (isMcLeaks(event.getName(), event.getUniqueId())) {
            BStatsHook.incrementBlockedMCLeaks();
            PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();
            BukkitCommandUtil.dispatchCommands(
                    playerManager.getMcLeaksCommands(event.getName(), event.getUniqueId(), ip),
                    Bukkit.getConsoleSender(),
                    plugin,
                    event.isAsynchronous()
            );
            Component kickMessage = playerManager.getMcLeaksKickMessage(event.getName(), event.getUniqueId(), ip);
            if (kickMessage != null) {
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(BukkitComponentSerializer.legacy().serialize(kickMessage));
            }
        }
    }
}

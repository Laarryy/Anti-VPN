package me.egg82.antivpn.events;

import me.egg82.antivpn.api.platform.BukkitPlatform;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.hooks.UpdaterHook;
import me.egg82.antivpn.locale.BukkitLocaleCommandUtil;
import me.egg82.antivpn.locale.LocaleUtil;
import me.egg82.antivpn.locale.MessageKey;
import ninja.egg82.events.BukkitEvents;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ExtraPlayerEvents extends EventHolder {
    public ExtraPlayerEvents(@NotNull Plugin plugin) {
        events.add(
                BukkitEvents.subscribe(plugin, PlayerJoinEvent.class, EventPriority.HIGH)
                        .handler(this::checkUpdate)
                        .exceptionHandler(ex -> logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex))
        );

        events.add(
                BukkitEvents.subscribe(plugin, PlayerLoginEvent.class, EventPriority.MONITOR)
                        .filter(e -> e.getResult() == PlayerLoginEvent.Result.ALLOWED)
                        .filter(e -> !Bukkit.hasWhitelist() || e.getPlayer().isWhitelisted())
                        .handler(e -> {
                            BukkitPlatform.addUniquePlayer(e.getPlayer().getUniqueId());
                            String ip = getIp(e.getAddress());
                            if (ip != null) {
                                try {
                                    BukkitPlatform.addUniqueIp(InetAddress.getByName(ip));
                                } catch (UnknownHostException ex) {
                                    logger.warn(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__NO_INET, "{ip}", ip), ex);
                                }
                            }
                        })
                        .exceptionHandler(ex -> logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex))
        );
    }

    private void checkUpdate(@NotNull PlayerJoinEvent event) {
        if (!event.getPlayer().hasPermission(ConfigUtil.getCachedConfig().getAdminPermissionNode())) {
            return;
        }

        UpdaterHook hook = UpdaterHook.get();
        if (hook != null) {
            hook.checkUpdate(BukkitLocaleCommandUtil.getSender(event.getPlayer()));
        }
    }
}

package me.egg82.antivpn.events;

import java.net.InetAddress;
import java.util.Optional;
import java.util.function.Consumer;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.hooks.PlaceholderAPIHook;
import me.egg82.antivpn.services.AnalyticsHelper;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.LogUtil;
import ninja.egg82.service.ServiceLocator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerLoginCheckHandler implements Consumer<PlayerLoginEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final VPNAPI api = VPNAPI.getInstance();

    public void accept(PlayerLoginEvent event) {
        String ip = getIp(event.getAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        Optional<Configuration> config = ConfigUtil.getConfig();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!config.isPresent() || !cachedConfig.isPresent()) {
            return;
        }

        if (event.getPlayer().hasPermission("avpn.bypass")) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.YELLOW + " bypasses check. Ignoring.");
            }
            return;
        }

        if (config.get().getNode("action", "kick-message").getString("").isEmpty() && config.get().getNode("action", "command").getString("").isEmpty()) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "Plugin set to API-only. Ignoring " + ChatColor.WHITE + event.getPlayer().getName());
            }
            return;
        }

        if (cachedConfig.get().getIgnoredIps().contains(ip)) {
            return;
        }

        boolean isVPN;

        if (config.get().getNode("action", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
            double consensus = clamp(0.0d, 1.0d, config.get().getNode("action", "algorithm", "min-consensus").getDouble(0.6d));
            try {
                isVPN = api.consensus(ip) >= consensus;
            } catch (APIException ex) {
                logger.error(ex.getMessage(), ex);
                isVPN = false;
            }
        } else {
            try {
                isVPN = api.cascade(ip);
            } catch (APIException ex) {
                logger.error(ex.getMessage(), ex);
                isVPN = false;
            }
        }

        if (isVPN) {
            AnalyticsHelper.incrementBlocked();
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.DARK_RED + " found using a VPN. Running required actions.");
            }

            tryRunCommand(config.get(), event.getPlayer());
            tryKickPlayer(config.get(), event.getPlayer(), event);
        } else {
            logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.GREEN + " passed VPN check.");
        }
    }

    private void tryRunCommand(Configuration config, Player player) {
        Optional<PlaceholderAPIHook> placeholderapi;
        try {
            placeholderapi = ServiceLocator.getOptional(PlaceholderAPIHook.class);
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
            placeholderapi = Optional.empty();
        }

        String command = config.getNode("action", "command").getString("");
        if (command.isEmpty()) {
            return;
        }

        command = command.replace("%player%", player.getName()).replace("%uuid%", player.getUniqueId().toString());
        if (command.charAt(0) == '/') {
            command = command.substring(1);
        }

        if (placeholderapi.isPresent()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholderapi.get().withPlaceholders(player, command));
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    private void tryKickPlayer(Configuration config, Player player, PlayerLoginEvent event) {
        Optional<PlaceholderAPIHook> placeholderapi;
        try {
            placeholderapi = ServiceLocator.getOptional(PlaceholderAPIHook.class);
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
            placeholderapi = Optional.empty();
        }

        String message = config.getNode("action", "kick-message").getString("");
        if (message.isEmpty()) {
            return;
        }

        event.setResult(PlayerLoginEvent.Result.KICK_OTHER);

        if (placeholderapi.isPresent()) {
            event.setKickMessage(placeholderapi.get().withPlaceholders(player, message));
        } else {
            event.setKickMessage(message);
        }
    }

    private String getIp(InetAddress address) {
        if (address == null) {
            return null;
        }

        return address.getHostAddress();
    }

    private double clamp(double min, double max, double val) { return Math.min(max, Math.max(min, val)); }
}

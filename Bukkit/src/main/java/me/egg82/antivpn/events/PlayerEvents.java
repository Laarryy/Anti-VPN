package me.egg82.antivpn.events;

import inet.ipaddr.IPAddressString;
import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.enums.VPNAlgorithmMethod;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.hooks.PlaceholderAPIHook;
import me.egg82.antivpn.services.AnalyticsHelper;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.events.BukkitEvents;
import ninja.egg82.service.ServiceLocator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;

public class PlayerEvents extends EventHolder {
    public PlayerEvents(Plugin plugin) {
        events.add(
                BukkitEvents.subscribe(plugin, AsyncPlayerPreLoginEvent.class, EventPriority.HIGH)
                        .handler(this::cachePlayer)
        );

        events.add(
                BukkitEvents.subscribe(plugin, PlayerLoginEvent.class, EventPriority.LOWEST)
                        .handler(this::checkPlayer)
        );
    }

    private void cachePlayer(AsyncPlayerPreLoginEvent event) {
        if (Bukkit.hasWhitelist() && !isWhitelisted(event.getUniqueId())) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getUniqueId() + ChatColor.YELLOW + " is not whitelisted while the server is in whitelist mode. Ignoring.");
            }
            return;
        }

        String ip = getIp(event.getAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            return;
        }

        for (String testAddress : cachedConfig.get().getIgnoredIps()) {
            if (ValidationUtil.isValidIp(testAddress) && ip.equalsIgnoreCase(testAddress)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getUniqueId() + ChatColor.YELLOW + " is using an ignored IP " + ChatColor.WHITE + ip + ChatColor.YELLOW + ". Ignoring.");
                }
                return;
            } else if (ValidationUtil.isValidIPRange(testAddress) && rangeContains(testAddress, ip)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getUniqueId() + ChatColor.YELLOW + " is under an ignored range " + ChatColor.WHITE + testAddress + " (" + ip + ")" + ChatColor.YELLOW + ". Ignoring.");
                }
                return;
            }
        }

        if ((!cachedConfig.get().getVPNKickMessage().isEmpty() || !cachedConfig.get().getVPNActionCommands().isEmpty())) {
            if (cachedConfig.get().getVPNAlgorithmMethod() == VPNAlgorithmMethod.CONSESNSUS) {
                try {
                    api.consensus(ip); // Calling this will cache the result internally, even if the value is unused
                } catch (APIException ex) {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                }
            } else {
                try {
                    api.cascade(ip); // Calling this will cache the result internally, even if the value is unused
                } catch (APIException ex) {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                }
            }
        }

        if (!cachedConfig.get().getMCLeaksKickMessage().isEmpty() || !cachedConfig.get().getMCLeaksActionCommands().isEmpty()) {
            try {
                api.isMCLeaks(event.getUniqueId()); // Calling this will cache the result internally, even if the value is unused
            } catch (APIException ex) {
                logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
            }
        }
    }

    private void checkPlayer(PlayerLoginEvent event) {
        if (Bukkit.hasWhitelist() && !event.getPlayer().isWhitelisted()) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getUniqueId() + ChatColor.YELLOW + " is not whitelisted while the server is in whitelist mode. Ignoring.");
            }
            return;
        }

        String ip = getIp(event.getAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            return;
        }

        if (event.getPlayer().hasPermission("avpn.bypass")) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.YELLOW + " bypasses check. Ignoring.");
            }
            return;
        }

        for (String testAddress : cachedConfig.get().getIgnoredIps()) {
            if (ValidationUtil.isValidIp(testAddress) && ip.equalsIgnoreCase(testAddress)) {
                return;
            } else if (ValidationUtil.isValidIPRange(testAddress) && rangeContains(testAddress, ip)) {
                return;
            }
        }

        if (!cachedConfig.get().getVPNKickMessage().isEmpty() || !cachedConfig.get().getVPNActionCommands().isEmpty()) {
            boolean isVPN;

            if (cachedConfig.get().getVPNAlgorithmMethod() == VPNAlgorithmMethod.CONSESNSUS) {
                try {
                    isVPN = api.consensus(ip) >= cachedConfig.get().getVPNAlgorithmConsensus();
                } catch (APIException ex) {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    isVPN = false;
                }
            } else {
                try {
                    isVPN = api.cascade(ip);
                } catch (APIException ex) {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    isVPN = false;
                }
            }

            if (isVPN) {
                AnalyticsHelper.incrementBlockedVPNs();
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.DARK_RED + " found using a VPN. Running required actions.");
                }

                tryRunCommands(cachedConfig.get().getVPNActionCommands(), event.getPlayer(), ip);
                tryKickPlayer(cachedConfig.get().getVPNKickMessage(), event.getPlayer(), event);
            } else {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.GREEN + " passed VPN check.");
                }
            }
        } else {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "VPN set to API-only. Ignoring VPN check for " + ChatColor.WHITE + event.getPlayer().getName());
            }
        }

        if (!cachedConfig.get().getMCLeaksKickMessage().isEmpty() || !cachedConfig.get().getMCLeaksActionCommands().isEmpty()) {
            boolean isMCLeaks;

            try {
                isMCLeaks = api.isMCLeaks(event.getPlayer().getUniqueId());
            } catch (APIException ex) {
                logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                isMCLeaks = false;
            }

            if (isMCLeaks) {
                AnalyticsHelper.incrementBlockedMCLeaks();
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.DARK_RED + " found using an MCLeaks account. Running required actions.");
                }

                tryRunCommands(cachedConfig.get().getMCLeaksActionCommands(), event.getPlayer(), ip);
                tryKickPlayer(cachedConfig.get().getMCLeaksKickMessage(), event.getPlayer(), event);
            } else {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.GREEN + " passed MCLeaks check.");
                }
            }
        } else {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "MCLeaks set to API-only. Ignoring MCLeaks check for " + ChatColor.WHITE + event.getPlayer().getName());
            }
        }
    }

    private void tryRunCommands(List<String> commands, Player player, String ip) {
        Optional<PlaceholderAPIHook> placeholderapi;
        try {
            placeholderapi = ServiceLocator.getOptional(PlaceholderAPIHook.class);
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
            placeholderapi = Optional.empty();
        }

        for (String command : commands) {
            command = command.replace("%player%", player.getName()).replace("%uuid%", player.getUniqueId().toString()).replace("%ip%", ip);
            if (command.charAt(0) == '/') {
                command = command.substring(1);
            }

            if (placeholderapi.isPresent()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholderapi.get().withPlaceholders(player, command));
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    private void tryKickPlayer(String message, Player player, PlayerLoginEvent event) {
        Optional<PlaceholderAPIHook> placeholderapi;
        try {
            placeholderapi = ServiceLocator.getOptional(PlaceholderAPIHook.class);
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
            placeholderapi = Optional.empty();
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

    private boolean isWhitelisted(UUID playerID) {
        for (OfflinePlayer p : Bukkit.getWhitelistedPlayers()) {
            if (playerID.equals(p.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    private boolean rangeContains(String range, String ip) { return new IPAddressString(range).contains(new IPAddressString(ip)); }
}

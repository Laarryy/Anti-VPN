package me.egg82.antivpn.events;

import inet.ipaddr.IPAddressString;
import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.hooks.LuckPermsHook;
import me.egg82.antivpn.hooks.PlaceholderAPIHook;
import me.egg82.antivpn.hooks.VaultHook;
import me.egg82.antivpn.hooks.plan.AnalyticsUtil;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.events.BukkitEvents;
import ninja.egg82.service.ServiceLocator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;

public class PlayerEvents extends EventHolder {

    public PlayerEvents(Plugin plugin) {
        events.add(
                BukkitEvents.subscribe(plugin, AsyncPlayerPreLoginEvent.class, EventPriority.HIGH)
                        .handler(this::checkPerms)
        );

        events.add(
                BukkitEvents.subscribe(plugin, PlayerLoginEvent.class, EventPriority.LOWEST)
                        .filter(e -> !Bukkit.hasWhitelist() || e.getPlayer().isWhitelisted())
                        .handler(this::checkPlayer)
        );
    }

    private void checkPerms(AsyncPlayerPreLoginEvent event) {
        if (Bukkit.hasWhitelist() && !isWhitelisted(event.getUniqueId())) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.HEADING + "<c1>" + event.getName() + " (" + event.getUniqueId() + ")</c1><c2>" + " is not whitelisted while the server is in whitelist mode. Ignoring.</c2>");
            }
            return;
        }

        Optional<LuckPermsHook> luckPermsHook;
        try {
            luckPermsHook = ServiceLocator.getOptional(LuckPermsHook.class);
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
            luckPermsHook = Optional.empty();
        }

        if (luckPermsHook.isPresent()) {
            // LuckPerms is available, run through entire check gambit
            checkPermsPlayer(event, luckPermsHook.get().hasPermission(event.getUniqueId(), "avpn.bypass"));
        } else {
            // LuckPerms is not available, check for Vault
            Optional<VaultHook> vaultHook;
            try {
                vaultHook = ServiceLocator.getOptional(VaultHook.class);
            } catch (InstantiationException | IllegalAccessException ex) {
                logger.error(ex.getMessage(), ex);
                vaultHook = Optional.empty();
            }

            if (vaultHook.isPresent() && vaultHook.get().getPermission() != null) {
                // Vault is available, run through entire check gambit
                checkPermsPlayer(event, vaultHook.get().getPermission().playerHas(null, Bukkit.getOfflinePlayer(event.getUniqueId()), "avpn.bypass"));
            } else {
                // Vault is not available, only cache data
                cachePlayer(event);
            }
        }
    }

    private void checkPermsPlayer(AsyncPlayerPreLoginEvent event, boolean hasBypass) {
        if (hasBypass) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.HEADING + "<c1>" + event.getName() + "</c1> <c2>bypasses pre-check. Ignoring.</c2>");
            }
            return;
        }

        String ip = getIp(event.getAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        // Check ignored IP addresses/ranges
        for (String testAddress : cachedConfig.getIgnoredIps()) {
            if (ValidationUtil.isValidIp(testAddress) && ip.equalsIgnoreCase(testAddress)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getName() + ChatColor.YELLOW + " is using an ignored IP " + ChatColor.WHITE + ip + ChatColor.YELLOW + ". Ignoring.");
                }
                return;
            } else if (ValidationUtil.isValidIPRange(testAddress) && rangeContains(testAddress, ip)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getName() + ChatColor.YELLOW + " is under an ignored range " + ChatColor.WHITE + testAddress + " (" + ip + ")" + ChatColor.YELLOW + ". Ignoring.");
                }
                return;
            }
        }

        cacheData(ip, event.getUniqueId(), cachedConfig.get());

        if (isVPN(ip, event.getName(), cachedConfig.get())) {
            AnalyticsUtil.incrementBlockedVPNs();
            if (!cachedConfig.get().getVPNActionCommands().isEmpty()) {
                tryRunCommands(cachedConfig.get().getVPNActionCommands(), event.getName(), event.getUniqueId(), ip);
            }
            if (!cachedConfig.get().getVPNKickMessage().isEmpty()) {
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(getKickMessage(cachedConfig.get().getVPNKickMessage(), event.getName(), event.getUniqueId(), ip));
            }
        }

        if (isMCLeaks(event.getName(), event.getUniqueId(), cachedConfig.get())) {
            AnalyticsUtil.incrementBlockedMCLeaks();
            if (!cachedConfig.get().getMCLeaksActionCommands().isEmpty()) {
                tryRunCommands(cachedConfig.get().getMCLeaksActionCommands(), event.getName(), event.getUniqueId(), ip);
            }
            if (!cachedConfig.get().getMCLeaksKickMessage().isEmpty()) {
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(getKickMessage(cachedConfig.get().getVPNKickMessage(), event.getName(), event.getUniqueId(), ip));
            }
        }
    }

    private void cachePlayer(AsyncPlayerPreLoginEvent event) {
        String ip = getIp(event.getAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        // Check ignored IP addresses/ranges
        for (String testAddress : cachedConfig.getIgnoredIps()) {
            if (
                    ValidationUtil.isValidIp(testAddress) && ip.equalsIgnoreCase(testAddress)
                    || ValidationUtil.isValidIPRange(testAddress) && rangeContains(testAddress, ip)
            ) {
                return;
            }
        }

        cacheData(ip, event.getUniqueId(), cachedConfig);
    }

    private void cacheData(String ip, UUID uuid, CachedConfig cachedConfig) {
        // Cache IP data
        if ((!cachedConfig.getVPNKickMessage().isEmpty() || !cachedConfig.getVPNActionCommands().isEmpty())) {
            if (cachedConfig.getVPNAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
                try {
                    api.consensus(ip); // Calling this will cache the result internally, even if the value is unused
                } catch (APIException ex) {
                    if (cachedConfig.getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
                }
            } else {
                try {
                    api.cascade(ip); // Calling this will cache the result internally, even if the value is unused
                } catch (APIException ex) {
                    if (cachedConfig.getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
                }
            }
        }

        // Cache MCLeaks data
        if (!cachedConfig.getMCLeaksKickMessage().isEmpty() || !cachedConfig.getMCLeaksActionCommands().isEmpty()) {
            try {
                api.isMCLeaks(uuid); // Calling this will cache the result internally, even if the value is unused
            } catch (APIException ex) {
                if (cachedConfig.getDebug()) {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                }
            }
        }
    }

    private void checkPlayer(PlayerLoginEvent event) {
        Optional<LuckPermsHook> luckPermsHook;
        try {
            luckPermsHook = ServiceLocator.getOptional(LuckPermsHook.class);
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
            luckPermsHook = Optional.empty();
        }

        if (luckPermsHook.isPresent()) {
            return;
        }

        Optional<VaultHook> vaultHook;
        try {
            vaultHook = ServiceLocator.getOptional(VaultHook.class);
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
            vaultHook = Optional.empty();
        }

        if (vaultHook.isPresent() && vaultHook.get().getPermission() != null) {
            return;
        }

        String ip = getIp(event.getAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        if (event.getPlayer().hasPermission("avpn.bypass")) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.YELLOW + " bypasses actions. Ignoring.");
            }
            return;
        }

        for (String testAddress : cachedConfig.get().getIgnoredIps()) {
            if (ValidationUtil.isValidIp(testAddress) && ip.equalsIgnoreCase(testAddress)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.YELLOW + " is using an ignored IP " + ChatColor.WHITE + ip + ChatColor.YELLOW + ". Ignoring.");
                }
                return;
            } else if (ValidationUtil.isValidIPRange(testAddress) && rangeContains(testAddress, ip)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.YELLOW + " is under an ignored range " + ChatColor.WHITE + testAddress + " (" + ip + ")" + ChatColor.YELLOW + ". Ignoring.");
                }
                return;
            }
        }

        if (isVPN(ip, event.getPlayer().getName(), cachedConfig.get())) {
            AnalyticsUtil.incrementBlockedVPNs();
            if (!cachedConfig.get().getVPNActionCommands().isEmpty()) {
                tryRunCommands(cachedConfig.get().getVPNActionCommands(), event.getPlayer().getName(), event.getPlayer().getUniqueId(), ip);
            }
            if (!cachedConfig.get().getVPNKickMessage().isEmpty()) {
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(getKickMessage(cachedConfig.get().getVPNKickMessage(), event.getPlayer().getName(), event.getPlayer().getUniqueId(), ip));
            }
        }

        if (isMCLeaks(event.getPlayer().getName(), event.getPlayer().getUniqueId(), cachedConfig.get())) {
            AnalyticsUtil.incrementBlockedMCLeaks();
            if (!cachedConfig.get().getMCLeaksActionCommands().isEmpty()) {
                tryRunCommands(cachedConfig.get().getMCLeaksActionCommands(), event.getPlayer().getName(), event.getPlayer().getUniqueId(), ip);
            }
            if (!cachedConfig.get().getMCLeaksKickMessage().isEmpty()) {
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(getKickMessage(cachedConfig.get().getVPNKickMessage(), event.getPlayer().getName(), event.getPlayer().getUniqueId(), ip));
            }
        }
    }

    private boolean isVPN(String ip, String name, CachedConfig cachedConfig) {
        if (!cachedConfig.getVPNKickMessage().isEmpty() || !cachedConfig.getVPNActionCommands().isEmpty()) {
            boolean isVPN;

            if (cachedConfig.getVPNAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
                try {
                    isVPN = api.consensus(ip) >= cachedConfig.getVPNAlgorithmConsensus();
                } catch (APIException ex) {
                    if (cachedConfig.getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
                    isVPN = false;
                }
            } else {
                try {
                    isVPN = api.cascade(ip);
                } catch (APIException ex) {
                    if (cachedConfig.getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
                    isVPN = false;
                }
            }

            if (isVPN) {
                if (cachedConfig.getDebug()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + name + ChatColor.DARK_RED + " found using a VPN. Running required actions.");
                }
            } else {
                if (cachedConfig.getDebug()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + name + ChatColor.GREEN + " passed VPN check.");
                }
            }
            return isVPN;
        } else {
            if (cachedConfig.getDebug()) {
                logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "VPN set to API-only. Ignoring VPN check for " + ChatColor.WHITE + name);
            }
        }

        return false;
    }

    private boolean isMCLeaks(String name, UUID uuid, CachedConfig cachedConfig) {
        if (!cachedConfig.getMCLeaksKickMessage().isEmpty() || !cachedConfig.getMCLeaksActionCommands().isEmpty()) {
            boolean isMCLeaks;

            try {
                isMCLeaks = api.isMCLeaks(uuid);
            } catch (APIException ex) {
                if (cachedConfig.getDebug()) {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                }
                isMCLeaks = false;
            }

            if (isMCLeaks) {
                if (cachedConfig.getDebug()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + name + ChatColor.DARK_RED + " found using an MCLeaks account. Running required actions.");
                }
            } else {
                if (cachedConfig.getDebug()) {
                    logger.info(LogUtil.getHeading() + ChatColor.WHITE + name + ChatColor.GREEN + " passed MCLeaks check.");
                }
            }
            return isMCLeaks;
        } else {
            if (cachedConfig.getDebug()) {
                logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "MCLeaks set to API-only. Ignoring MCLeaks check for " + ChatColor.WHITE + name);
            }
        }

        return false;
    }

    private void tryRunCommands(List<String> commands, String name, UUID uuid, String ip) {
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
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholderapi.get().withPlaceholders(Bukkit.getOfflinePlayer(uuid), command));
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    private String getKickMessage(String message, String name, UUID uuid, String ip) {
        Optional<PlaceholderAPIHook> placeholderapi;
        try {
            placeholderapi = ServiceLocator.getOptional(PlaceholderAPIHook.class);
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
            placeholderapi = Optional.empty();
        }

        message = message.replace("%player%", name).replace("%uuid%", uuid.toString()).replace("%ip%", ip);
        if (placeholderapi.isPresent()) {
            message = placeholderapi.get().withPlaceholders(Bukkit.getOfflinePlayer(uuid), message);
        }
        return message;
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
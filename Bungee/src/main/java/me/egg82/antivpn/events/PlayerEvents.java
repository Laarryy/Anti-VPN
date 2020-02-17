package me.egg82.antivpn.events;

import inet.ipaddr.IPAddressString;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.enums.VPNAlgorithmMethod;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.services.AnalyticsHelper;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventPriority;
import ninja.egg82.events.BungeeEvents;

public class PlayerEvents extends EventHolder {
    public PlayerEvents(Plugin plugin) {
        events.add(
                BungeeEvents.subscribe(plugin, PreLoginEvent.class, EventPriority.HIGH)
                        .handler(this::cachePlayer)
        );

        events.add(
                BungeeEvents.subscribe(plugin, PostLoginEvent.class, EventPriority.LOWEST)
                        .handler(this::checkPlayer)
        );
    }

    private void cachePlayer(PreLoginEvent event) {
        String ip = getIp(event.getConnection().getAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            return;
        }

        for (String testAddress : cachedConfig.get().getIgnoredIps()) {
            if (
                    ValidationUtil.isValidIp(testAddress) && ip.equalsIgnoreCase(testAddress)
                    || ValidationUtil.isValidIPRange(testAddress) && rangeContains(testAddress, ip)
            ) {
                return;
            }
        }

        if ((!cachedConfig.get().getVPNKickMessage().isEmpty() || !cachedConfig.get().getVPNActionCommands().isEmpty())) {
            if (cachedConfig.get().getVPNAlgorithmMethod() == VPNAlgorithmMethod.CONSESNSUS) {
                try {
                    api.consensus(ip); // Calling this will cache the result internally, even if the value is unused
                } catch (APIException ex) {
                    if (cachedConfig.get().getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
                }
            } else {
                try {
                    api.cascade(ip); // Calling this will cache the result internally, even if the value is unused
                } catch (APIException ex) {
                    if (cachedConfig.get().getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
                }
            }
        }

        // Can't cache MCleaks - event.getConnection().getUniqueId() is null here
    }

    private void checkPlayer(PostLoginEvent event) {
        String ip = getIp(event.getPlayer().getAddress());
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

        if (!cachedConfig.get().getVPNKickMessage().isEmpty() || !cachedConfig.get().getVPNActionCommands().isEmpty()) {
            boolean isVPN;

            if (cachedConfig.get().getVPNAlgorithmMethod() == VPNAlgorithmMethod.CONSESNSUS) {
                try {
                    isVPN = api.consensus(ip) >= cachedConfig.get().getVPNAlgorithmConsensus();
                } catch (APIException ex) {
                    if (cachedConfig.get().getDebug()) {
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
                    if (cachedConfig.get().getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
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
                if (cachedConfig.get().getDebug()) {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                }
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

    private void tryRunCommands(List<String> commands, ProxiedPlayer player, String ip) {
        for (String command : commands) {
            command = command.replace("%player%", player.getName()).replace("%uuid%", player.getUniqueId().toString()).replace("%ip%", ip);
            if (command.charAt(0) == '/') {
                command = command.substring(1);
            }

            ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), command);
        }
    }

    private void tryKickPlayer(String message, ProxiedPlayer player, PostLoginEvent event) {
        player.disconnect(TextComponent.fromLegacyText(message.replace('&', ChatColor.COLOR_CHAR)));
    }

    private String getIp(InetSocketAddress address) {
        if (address == null) {
            return null;
        }
        InetAddress host = address.getAddress();
        if (host == null) {
            return null;
        }
        return host.getHostAddress();
    }

    private boolean rangeContains(String range, String ip) { return new IPAddressString(range).contains(new IPAddressString(ip)); }
}

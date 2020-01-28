package me.egg82.antivpn.events;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import inet.ipaddr.IPAddressString;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.enums.VPNAlgorithmMethod;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.services.AnalyticsHelper;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import ninja.egg82.events.VelocityEvents;

public class PlayerEvents extends EventHolder {
    private final ProxyServer proxy;

    public PlayerEvents(Object plugin, ProxyServer proxy) {
        this.proxy = proxy;

        events.add(
                VelocityEvents.subscribe(plugin, proxy, PreLoginEvent.class, PostOrder.LATE)
                        .handler(this::cachePlayer)
        );

        events.add(
                VelocityEvents.subscribe(plugin, proxy, PostLoginEvent.class, PostOrder.FIRST)
                        .handler(this::checkPlayer)
        );
    }

    private void cachePlayer(PreLoginEvent event) {
        String ip = getIp(event.getConnection().getRemoteAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            return;
        }

        UUID playerID = getPlayerUUID(event.getUsername(), proxy);
        if (playerID == null) {
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of(ip).color(TextColor.WHITE)).append(TextComponent.of(" is using an invalid player name ").color(TextColor.YELLOW)).append(TextComponent.of(event.getUsername()).color(TextColor.WHITE)).append(TextComponent.of(". Skipping pre-login cache and letting Velocity validate the request before checking.").color(TextColor.YELLOW)).build());
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
                api.isMCLeaks(playerID); // Calling this will cache the result internally, even if the value is unused
            } catch (APIException ex) {
                logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
            }
        }
    }

    private void checkPlayer(PostLoginEvent event) {
        String ip = getIp(event.getPlayer().getRemoteAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            return;
        }

        if (event.getPlayer().hasPermission("avpn.bypass")) {
            if (ConfigUtil.getDebugOrFalse()) {
                proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of(event.getPlayer().getUsername()).color(TextColor.WHITE)).append(TextComponent.of(" bypasses check. Ignoring.").color(TextColor.YELLOW)).build());
            }
            return;
        }

        for (String testAddress : cachedConfig.get().getIgnoredIps()) {
            if (ValidationUtil.isValidIp(testAddress) && ip.equalsIgnoreCase(testAddress)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of(event.getPlayer().getUsername()).color(TextColor.WHITE)).append(TextComponent.of(" is using an ignored IP ").color(TextColor.YELLOW)).append(TextComponent.of(ip).color(TextColor.WHITE)).append(TextComponent.of(". Ignoring.").color(TextColor.YELLOW)).build());
                }
                return;
            } else if (ValidationUtil.isValidIPRange(testAddress) && rangeContains(testAddress, ip)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of(event.getPlayer().getUsername()).color(TextColor.WHITE)).append(TextComponent.of(" is under an ignored range ").color(TextColor.YELLOW)).append(TextComponent.of(testAddress + " (" + ip + ")").color(TextColor.WHITE)).append(TextComponent.of(". Ignoring.").color(TextColor.YELLOW)).build());
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
                    proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of(event.getPlayer().getUsername()).color(TextColor.WHITE)).append(TextComponent.of(" found using a VPN. Running required actions.").color(TextColor.DARK_RED)).build());
                }

                tryRunCommands(cachedConfig.get().getVPNActionCommands(), event.getPlayer(), ip);
                tryKickPlayer(cachedConfig.get().getVPNKickMessage(), event.getPlayer(), event);
            } else {
                if (ConfigUtil.getDebugOrFalse()) {
                    proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of(event.getPlayer().getUsername()).color(TextColor.WHITE)).append(TextComponent.of(" passed VPN check.").color(TextColor.GREEN)).build());
                }
            }
        } else {
            if (ConfigUtil.getDebugOrFalse()) {
                proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("VPN set to API-only. Ignoring VPN check for ").color(TextColor.YELLOW)).append(TextComponent.of(event.getPlayer().getUsername()).color(TextColor.WHITE)).build());
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
                    proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of(event.getPlayer().getUsername()).color(TextColor.WHITE)).append(TextComponent.of(" found using an MCLeaks account. Running required actions.").color(TextColor.DARK_RED)).build());
                }

                tryRunCommands(cachedConfig.get().getMCLeaksActionCommands(), event.getPlayer(), ip);
                tryKickPlayer(cachedConfig.get().getMCLeaksKickMessage(), event.getPlayer(), event);
            } else {
                if (ConfigUtil.getDebugOrFalse()) {
                    proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of(event.getPlayer().getUsername()).color(TextColor.WHITE)).append(TextComponent.of(" passed MCLeaks check.").color(TextColor.GREEN)).build());
                }
            }
        } else {
            if (ConfigUtil.getDebugOrFalse()) {
                proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("MCLeaks set to API-only. Ignoring MCLeaks check for ").color(TextColor.YELLOW)).append(TextComponent.of(event.getPlayer().getUsername()).color(TextColor.WHITE)).build());
            }
        }
    }

    private void tryRunCommands(List<String> commands, Player player, String ip) {
        for (String command : commands) {
            command = command.replace("%player%", player.getUsername()).replace("%uuid%", player.getUniqueId().toString()).replace("%ip%", ip);
            if (command.charAt(0) == '/') {
                command = command.substring(1);
            }

            proxy.getCommandManager().execute(proxy.getConsoleCommandSource(), command);
        }
    }

    private UUID getPlayerUUID(String name, ProxyServer proxy) {
        PlayerInfo info;
        try {
            info = PlayerLookup.get(name, proxy);
        } catch (IOException ex) {
            logger.warn("Could not fetch player UUID. (rate-limited?)", ex);
            return null;
        }
        return info.getUUID();
    }

    private void tryKickPlayer(String message, Player player, PostLoginEvent event) {
        player.disconnect(TextComponent.of(message));
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

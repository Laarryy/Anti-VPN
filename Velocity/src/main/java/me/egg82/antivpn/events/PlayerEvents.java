package me.egg82.antivpn.events;

import co.aikar.commands.CommandIssuer;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import inet.ipaddr.IPAddressString;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import me.egg82.antivpn.AntiVPN;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.api.platform.VelocityPlatform;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.hooks.LuckPermsHook;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import me.egg82.antivpn.utils.ExceptionUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ninja.egg82.events.VelocityEvents;
import ninja.egg82.service.ServiceLocator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PlayerEvents extends EventHolder {
    private final ProxyServer proxy;
    private final CommandIssuer console;

    public PlayerEvents(@NonNull Object plugin, @NonNull ProxyServer proxy, @NonNull CommandIssuer console) {
        this.proxy = proxy;
        this.console = console;

        events.add(
                VelocityEvents.subscribe(plugin, proxy, PreLoginEvent.class, PostOrder.LATE)
                        .handler(this::checkPerms)
        );

        events.add(
                VelocityEvents.subscribe(plugin, proxy, PostLoginEvent.class, PostOrder.FIRST)
                        .handler(this::checkPlayer)
        );

        events.add(
                VelocityEvents.subscribe(plugin, proxy, PostLoginEvent.class, PostOrder.LAST)
                        .handler(e -> {
                            VelocityPlatform.addUniquePlayer(e.getPlayer().getUniqueId());
                            try {
                                VelocityPlatform.addUniqueIp(InetAddress.getByName(getIp(e.getPlayer().getRemoteAddress())));
                            } catch (UnknownHostException ex) {
                                logger.warn("Could not create InetAddress for " + getIp(e.getPlayer().getRemoteAddress()));
                            }
                        })
        );
    }

    private void checkPerms(@NonNull PreLoginEvent event) {
        Optional<LuckPermsHook> luckPermsHook;
        try {
            luckPermsHook = ServiceLocator.getOptional(LuckPermsHook.class);
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getMessage(), ex);
            luckPermsHook = Optional.empty();
        }

        if (luckPermsHook.isPresent()) {
            UUID uuid = fetchUuid(event.getUsername());
            if (uuid != null) {
                // LuckPerms + UUID is available, run through entire check gambit
                checkPermsPlayer(event, uuid, luckPermsHook.get().hasPermission(uuid, "avpn.bypass"));
            } else {
                // LuckPerms is available but UUID is not, only cache data
                cachePlayer(event, uuid);
            }
        } else {
            // LuckPerms is not available, only cache data
            cachePlayer(event, fetchUuid(event.getUsername()));
        }
    }

    private void checkPermsPlayer(@NonNull PreLoginEvent event, @NonNull UUID uuid,  boolean hasBypass) {
        if (hasBypass) {
            if (ConfigUtil.getDebugOrFalse()) {
                console.sendMessage("<c1>" + event.getUsername() + "</c1> <c2>bypasses pre-check. Ignoring.</c2>");
            }
            return;
        }

        String ip = getIp(event.getConnection().getRemoteAddress());
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
                    console.sendMessage("<c1>" + event.getUsername() + "</c1> <c2>is using an ignored IP</c2> <c1>" + ip + "</c1><c2>. Ignoring.</c2>");
                }
                return;
            } else if (ValidationUtil.isValidIpRange(testAddress) && rangeContains(testAddress, ip)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    console.sendMessage("<c1>" + event.getUsername() + "</c1> <c2>is under an ignored range</c2> <c1>" + testAddress + " (" + ip + ")" + "</c1><c2>. Ignoring.</c2>");
                }
                return;
            }
        }

        cacheData(ip, uuid, cachedConfig);

        if (isVpn(ip, event.getUsername(), cachedConfig)) {
            AntiVPN.incrementBlockedVPNs();
            IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();
            List<String> commands = ipManager.getVpnCommands(event.getUsername(), uuid, ip);
            for (String command : commands) {
                try {
                    proxy.getCommandManager().executeImmediatelyAsync(proxy.getConsoleCommandSource(), command).join();
                } catch (CancellationException | CompletionException ex) {
                    ExceptionUtil.handleException(ex, logger);
                }
            }
            String kickMessage = ipManager.getVpnKickMessage(event.getUsername(), uuid, ip);
            if (kickMessage != null) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(LegacyComponentSerializer.legacyAmpersand().deserialize(kickMessage)));
            }
        }

        if (isMcLeaks(event.getUsername(), uuid, cachedConfig)) {
            AntiVPN.incrementBlockedMCLeaks();
            PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();
            List<String> commands = playerManager.getMcLeaksCommands(event.getUsername(), uuid, ip);
            for (String command : commands) {
                proxy.getCommandManager().executeImmediatelyAsync(proxy.getConsoleCommandSource(), command);
            }
            String kickMessage = playerManager.getMcLeaksKickMessage(event.getUsername(), uuid, ip);
            if (kickMessage != null) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(LegacyComponentSerializer.legacyAmpersand().deserialize(kickMessage)));
            }
        }
    }

    private void cachePlayer(@NonNull PreLoginEvent event, UUID uuid) {
        if (uuid == null) {
            return;
        }

        String ip = getIp(event.getConnection().getRemoteAddress());
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
                    || ValidationUtil.isValidIpRange(testAddress) && rangeContains(testAddress, ip)
            ) {
                return;
            }
        }

        cacheData(ip, uuid, cachedConfig);
    }

    private void cacheData(@NonNull String ip, @NonNull UUID uuid, @NonNull CachedConfig cachedConfig) {
        // Cache IP data
        if ((!cachedConfig.getVPNKickMessage().isEmpty() || !cachedConfig.getVPNActionCommands().isEmpty())) {
            IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();
            if (cachedConfig.getVPNAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
                try {
                    ipManager.consensus(ip, true).get(); // Calling this will cache the result internally, even if the value is unused
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | CancellationException ex) {
                    ExceptionUtil.handleException(ex, logger);
                }
            } else {
                try {
                    ipManager.cascade(ip, true).get(); // Calling this will cache the result internally, even if the value is unused
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | CancellationException ex) {
                    ExceptionUtil.handleException(ex, logger);
                }
            }
        }

        // Cache MCLeaks data
        if (!cachedConfig.getMCLeaksKickMessage().isEmpty() || !cachedConfig.getMCLeaksActionCommands().isEmpty()) {
            PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();
            try {
                playerManager.checkMcLeaks(uuid, true).get(); // Calling this will cache the result internally, even if the value is unused
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | CancellationException ex) {
                ExceptionUtil.handleException(ex, logger);
            }
        }
    }

    private void checkPlayer(@NonNull PostLoginEvent event) {
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

        String ip = getIp(event.getPlayer().getRemoteAddress());
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
                console.sendMessage("<c1>" + event.getPlayer().getUsername() + "</c1> <c2>bypasses actions. Ignoring.</c2>");
            }
            return;
        }

        for (String testAddress : cachedConfig.getIgnoredIps()) {
            if (ValidationUtil.isValidIp(testAddress) && ip.equalsIgnoreCase(testAddress)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    console.sendMessage("<c1>" + event.getPlayer().getUsername() + "</c1> <c2>is using an ignored IP</c2> <c1>" + ip + "</c1><c2>. Ignoring.</c2>");
                }
                return;
            } else if (ValidationUtil.isValidIpRange(testAddress) && rangeContains(testAddress, ip)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    console.sendMessage("<c1>" + event.getPlayer().getUsername() + "</c1> <c2>is under an ignored range</c2> <c1>" + testAddress + " (" + ip + ")" + "</c1><c2>. Ignoring.</c2>");
                }
                return;
            }
        }

        if (isVpn(ip, event.getPlayer().getUsername(), cachedConfig)) {
            AntiVPN.incrementBlockedVPNs();
            IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();
            List<String> commands = ipManager.getVpnCommands(event.getPlayer().getUsername(), event.getPlayer().getUniqueId(), ip);
            for (String command : commands) {
                proxy.getCommandManager().executeImmediatelyAsync(proxy.getConsoleCommandSource(), command);
            }
            String kickMessage = ipManager.getVpnKickMessage(event.getPlayer().getUsername(), event.getPlayer().getUniqueId(), ip);
            if (kickMessage != null) {
                event.getPlayer().disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(kickMessage));
            }
        }

        if (isMcLeaks(event.getPlayer().getUsername(), event.getPlayer().getUniqueId(), cachedConfig)) {
            AntiVPN.incrementBlockedMCLeaks();
            PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();
            List<String> commands = playerManager.getMcLeaksCommands(event.getPlayer().getUsername(), event.getPlayer().getUniqueId(), ip);
            for (String command : commands) {
                proxy.getCommandManager().executeImmediatelyAsync(proxy.getConsoleCommandSource(), command);
            }
            String kickMessage = playerManager.getMcLeaksKickMessage(event.getPlayer().getUsername(), event.getPlayer().getUniqueId(), ip);
            if (kickMessage != null) {
                event.getPlayer().disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(kickMessage));
            }
        }
    }

    private boolean isVpn(@NonNull String ip, @NonNull String name, @NonNull CachedConfig cachedConfig) {
        if (!cachedConfig.getVPNKickMessage().isEmpty() || !cachedConfig.getVPNActionCommands().isEmpty()) {
            boolean isVPN;

            IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();
            if (cachedConfig.getVPNAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
                try {
                    Double val = ipManager.consensus(ip, true).get();
                    isVPN = val != null && val >= cachedConfig.getVPNAlgorithmConsensus();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    isVPN = false;
                } catch (ExecutionException | CancellationException ex) {
                    ExceptionUtil.handleException(ex, logger);
                    isVPN = false;
                }
            } else {
                try {
                    isVPN = Boolean.TRUE.equals(ipManager.cascade(ip, true).get());
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    isVPN = false;
                } catch (ExecutionException | CancellationException ex) {
                    ExceptionUtil.handleException(ex, logger);
                    isVPN = false;
                }
            }

            if (isVPN) {
                if (cachedConfig.getDebug()) {
                    console.sendMessage("<c1>" + name + "</c1> <c9>found using a VPN. Running required actions.</c9>");
                }
            } else {
                if (cachedConfig.getDebug()) {
                    console.sendMessage("<c1>" + name + "</c1> <c4>passed VPN check.</c4>");
                }
            }
            return isVPN;
        } else {
            if (cachedConfig.getDebug()) {
                console.sendMessage("<c2>Plugin set to API-only. Ignoring VPN check for</c2> <c1>" + name + "</c1>");
            }
        }

        return false;
    }

    private boolean isMcLeaks(@NonNull String name, @NonNull UUID uuid, @NonNull CachedConfig cachedConfig) {
        if (!cachedConfig.getMCLeaksKickMessage().isEmpty() || !cachedConfig.getMCLeaksActionCommands().isEmpty()) {
            boolean isMCLeaks;

            PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();
            try {
                isMCLeaks = Boolean.TRUE.equals(playerManager.checkMcLeaks(uuid, true).get());
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                isMCLeaks = false;
            } catch (ExecutionException | CancellationException ex) {
                ExceptionUtil.handleException(ex, logger);
                isMCLeaks = false;
            }

            if (isMCLeaks) {
                if (cachedConfig.getDebug()) {
                    console.sendMessage("<c1>" + name + "</c1> <c9>found using an MCLeaks account. Running required actions.</c9>");
                }
            } else {
                if (cachedConfig.getDebug()) {
                    console.sendMessage("<c1>" + name + "</c1> <c4>passed MCLeaks check.</c4>");
                }
            }
            return isMCLeaks;
        } else {
            if (cachedConfig.getDebug()) {
                console.sendMessage("<c2>Plugin set to API-only. Ignoring MCLeaks check for</c2> <c1>" + name + "</c1>");
            }
        }

        return false;
    }

    private @Nullable String getIp(InetSocketAddress address) {
        if (address == null) {
            return null;
        }
        InetAddress host = address.getAddress();
        if (host == null) {
            return null;
        }
        return host.getHostAddress();
    }

    private @Nullable UUID fetchUuid(@NonNull String name) {
        PlayerInfo info;
        try {
            info = PlayerLookup.get(name, proxy);
        } catch (IOException ex) {
            logger.warn("Could not fetch player UUID. (rate-limited?)", ex);
            return null;
        }
        return info.getUUID();
    }

    private boolean rangeContains(@NonNull String range, @NonNull String ip) { return new IPAddressString(range).contains(new IPAddressString(ip)); }
}

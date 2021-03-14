package me.egg82.antivpn.events;

import inet.ipaddr.IPAddressString;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.locale.BukkitLocaleCommandUtil;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.events.BukkitEventSubscriber;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public abstract class EventHolder {
    protected final Logger logger = new GELFLogger(LoggerFactory.getLogger(getClass()));

    protected final List<BukkitEventSubscriber<?>> events = new ArrayList<>();

    public final int numEvents() { return events.size(); }

    public final void cancel() {
        for (BukkitEventSubscriber<?> event : events) {
            event.cancel();
        }
    }

    protected final @Nullable String getIp(@Nullable InetAddress address) {
        if (address == null) {
            return null;
        }
        return address.getHostAddress();
    }

    protected final boolean isWhitelisted(@Nullable UUID playerID) {
        if (playerID == null) {
            return false;
        }
        for (OfflinePlayer p : Bukkit.getWhitelistedPlayers()) {
            if (playerID.equals(p.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    protected final boolean rangeContains(@NotNull String range, @NotNull String ip) { return new IPAddressString(range).contains(new IPAddressString(ip)); }

    protected final boolean isIgnoredIp(@NotNull String ip, @NotNull String playerName, @NotNull UUID playerId) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        // Check ignored IP addresses/ranges
        for (String testAddress : cachedConfig.getIgnoredIps()) {
            if (ValidationUtil.isValidIp(testAddress) && ip.equalsIgnoreCase(testAddress)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    BukkitLocaleCommandUtil.getConsole().sendMessage(
                            MessageKey.DEBUG__IGNORED_IP,
                            "{name}", playerName,
                            "{uuid}", playerId.toString(),
                            "{ip}", ip
                    );
                }
                return true;
            } else if (ValidationUtil.isValidIpRange(testAddress) && rangeContains(testAddress, ip)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    BukkitLocaleCommandUtil.getConsole().sendMessage(
                            MessageKey.DEBUG__IGNORED_RANGE,
                            "{name}", playerName,
                            "{uuid}", playerId.toString(),
                            "{ip}", ip,
                            "{range}", testAddress
                    );
                }
                return true;
            }
        }

        return false;
    }

    protected final boolean getVpnDataBlocking(@NotNull String ip) {
        IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();
        if (ipManager.getCurrentAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
            try {
                return ipManager.consensus(ip, true).get() >= ipManager.getMinConsensusValue();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | CancellationException ex) {
                logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        } else {
            try {
                return ipManager.cascade(ip, true).get();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | CancellationException ex) {
                logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }

        return false;
    }

    protected final boolean getMcLeaksDataBlocking(@NotNull UUID playerId) {
        PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();
        try {
            return playerManager.checkMcLeaks(playerId, true).get();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | CancellationException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }

        return false;
    }

    protected final void cacheData(@NotNull String ip, @NotNull UUID playerId) {
        // Cache IP data
        getVpnDataBlocking(ip); // Calling this will cache the result internally, even if the value is unused

        // Cache MCLeaks data
        getMcLeaksDataBlocking(playerId); // Calling this will cache the result internally, even if the value is unused
    }

    protected final boolean isVpn(@NotNull String ip, @NotNull String playerName, @NotNull UUID playerId) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (!cachedConfig.getVPNKickMessage().isEmpty() || !cachedConfig.getVPNActionCommands().isEmpty()) {
            boolean isVPN = getVpnDataBlocking(ip);
            if (cachedConfig.getDebug()) {
                if (isVPN) {
                    BukkitLocaleCommandUtil.getConsole().sendMessage(
                            MessageKey.DEBUG__VPN_DETECTED,
                            "{name}", playerName,
                            "{uuid}", playerId.toString(),
                            "{ip}", ip
                    );
                } else {
                    BukkitLocaleCommandUtil.getConsole().sendMessage(
                            MessageKey.DEBUG__VPN_PASSED,
                            "{name}", playerName,
                            "{uuid}", playerId.toString(),
                            "{ip}", ip
                    );
                }
            }
            return isVPN;
        } else {
            if (cachedConfig.getDebug()) {
                BukkitLocaleCommandUtil.getConsole().sendMessage(
                        MessageKey.DEBUG__VPN_API_ONLY,
                        "{name}", playerName,
                        "{uuid}", playerId.toString(),
                        "{ip}", ip
                );
            }
        }

        return false;
    }

    protected final boolean isMcLeaks(@NotNull String playerName, @NotNull UUID playerId) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (!cachedConfig.getMCLeaksKickMessage().isEmpty() || !cachedConfig.getMCLeaksActionCommands().isEmpty()) {
            boolean isMCLeaks = getMcLeaksDataBlocking(playerId);
            if (cachedConfig.getDebug()) {
                if (isMCLeaks) {
                    BukkitLocaleCommandUtil.getConsole().sendMessage(
                            MessageKey.DEBUG__MCLEAKS_DETECTED,
                            "{name}", playerName,
                            "{uuid}", playerId.toString()
                    );
                } else {
                    BukkitLocaleCommandUtil.getConsole().sendMessage(
                            MessageKey.DEBUG__MCLEAKS_PASSED,
                            "{name}", playerName,
                            "{uuid}", playerId.toString()
                    );
                }
            }
            return isMCLeaks;
        } else {
            if (cachedConfig.getDebug()) {
                BukkitLocaleCommandUtil.getConsole().sendMessage(
                        MessageKey.DEBUG__MCLEAKS_API_ONLY,
                        "{name}", playerName,
                        "{uuid}", playerId.toString()
                );
            }
        }

        return false;
    }
}

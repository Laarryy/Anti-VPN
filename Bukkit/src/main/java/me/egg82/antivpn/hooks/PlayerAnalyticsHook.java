package me.egg82.antivpn.hooks;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.ExtensionService;
import com.djrapitops.plan.extension.FormatType;
import com.djrapitops.plan.extension.annotation.BooleanProvider;
import com.djrapitops.plan.extension.annotation.NumberProvider;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.config.ConfigUtil;
import ninja.egg82.events.BukkitEvents;
import ninja.egg82.service.ServiceLocator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerAnalyticsHook implements PluginHook {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CapabilityService capabilities;

    public static void create(@NonNull Plugin plugin, @NonNull Plugin plan) {
        if (!plan.isEnabled()) {
            BukkitEvents.subscribe(plugin, PluginEnableEvent.class, EventPriority.MONITOR)
                    .expireIf(e -> e.getPlugin().getName().equals("Plan"))
                    .filter(e -> e.getPlugin().getName().equals("Plan"))
                    .handler(e -> ServiceLocator.register(new PlayerAnalyticsHook()));
            return;
        }
        ServiceLocator.register(new PlayerAnalyticsHook());
    }

    private PlayerAnalyticsHook() {
        capabilities = CapabilityService.getInstance();

        if (isCapabilityAvailable("DATA_EXTENSION_VALUES") && isCapabilityAvailable("DATA_EXTENSION_TABLES")) {
            try {
                ExtensionService.getInstance().register(new Data());
            } catch (NoClassDefFoundError ex) {
                // Plan not installed
                logger.error("Plan is not installed.", ex);
            } catch (IllegalStateException ex) {
                // Plan not enabled
                logger.error("Plan is not enabled.", ex);
            } catch (IllegalArgumentException ex) {
                // DataExtension impl error
                logger.error("DataExtension implementation exception.", ex);
            }
        }
    }

    public void cancel() { }

    private boolean isCapabilityAvailable(@NonNull String capability) {
        try {
            return capabilities.hasCapability(capability);
        } catch (NoClassDefFoundError ignored) {
            return false;
        }
    }

    @PluginInfo(
            name = "AntiVPN",
            iconName = "shield-alt",
            iconFamily = Family.SOLID,
            color = Color.BLUE
    )
    class Data implements DataExtension {
        private final CallEvents[] events = new CallEvents[] { CallEvents.SERVER_PERIODICAL, CallEvents.SERVER_EXTENSION_REGISTER, CallEvents.PLAYER_JOIN };

        private Data() { }

        @NumberProvider(
                text = "VPN Users",
                description = "Number of online VPN users.",
                priority = 2,
                iconName = "user-shield",
                iconFamily = Family.SOLID,
                iconColor = Color.NONE,
                format = FormatType.NONE
        )
        public long getVpns() {
            IPManager ipManager = VPNAPIProvider.getInstance().getIpManager();

            // TODO: multi-thread this
            long retVal = 0L;
            for (Player p : Bukkit.getOnlinePlayers()) {
                String ip = getIp(p);
                if (ip == null || ip.isEmpty()) {
                    continue;
                }

                if (ipManager.getCurrentAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
                    try {
                        if (ipManager.consensus(ip, true)
                                .exceptionally(this::handleException)
                                .join() >= ipManager.getMinConsensusValue()) {
                            retVal += 1;
                        }
                    } catch (Exception ignored) { }
                } else {
                    try {
                        if (ipManager.cascade(ip, true)
                                .exceptionally(this::handleException)
                                .join()) {
                            retVal += 1;
                        }
                    } catch (Exception ignored) { }
                }
            }
            return retVal;
        }

        @NumberProvider(
                text = "MCLeaks Users",
                description = "Number of online MCLeaks users.",
                priority = 1,
                iconName = "users",
                iconFamily = Family.SOLID,
                iconColor = Color.NONE,
                format = FormatType.NONE
        )
        public long getMcLeaks() {
            PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();

            // TODO: multi-thread this
            long retVal = 0L;
            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    if (playerManager.checkMcLeaks(p.getUniqueId(), true)
                            .exceptionally(this::handleException)
                            .join()) {
                        retVal += 1;
                    }
                } catch (Exception ignored) { }
            }
            return retVal;
        }

        @BooleanProvider(
                text = "VPN",
                description = "Using a VPN or proxy.",
                iconName = "user-shield",
                iconFamily = Family.SOLID,
                iconColor = Color.NONE
        )
        public boolean getUsingVpn(@NonNull UUID playerID) {
            Player player = Bukkit.getPlayer(playerID);
            if (player == null) {
                return false;
            }

            String ip = getIp(player);
            if (ip == null || ip.isEmpty()) {
                return false;
            }

            IPManager ipManager = VPNAPIProvider.getInstance().getIpManager();

            if (ipManager.getCurrentAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
                try {
                    return ipManager.consensus(ip, true)
                            .exceptionally(this::handleException)
                            .join() >= ipManager.getMinConsensusValue();
                } catch (Exception ignored) { }
            } else {
                try {
                    return ipManager.cascade(ip, true)
                            .exceptionally(this::handleException)
                            .join();
                } catch (Exception ignored) { }
            }

            return false;
        }

        @BooleanProvider(
                text = "MCLeaks",
                description = "Using an MCLeaks account.",
                iconName = "users",
                iconFamily = Family.SOLID,
                iconColor = Color.NONE
        )
        public boolean getMcLeaks(@NonNull UUID playerId) {
            PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();

            try {
                return playerManager.checkMcLeaks(playerId, true)
                        .exceptionally(this::handleException)
                        .join();
            } catch (Exception ignored) { }

            return false;
        }

        private @Nullable String getIp(@NonNull Player player) {
            InetSocketAddress address = player.getAddress();
            if (address == null) {
                return null;
            }
            InetAddress host = address.getAddress();
            if (host == null) {
                return null;
            }
            return host.getHostAddress();
        }

        private <T> @Nullable T handleException(@NonNull Throwable ex) {
            Throwable oldEx = null;
            if (ex instanceof CompletionException) {
                oldEx = ex;
                ex = ex.getCause();
            }

            if (ex instanceof APIException) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.error("[Hard: " + ((APIException) ex).isHard() + "] " + ex.getMessage(), oldEx != null ? oldEx : ex);
                } else {
                    logger.error("[Hard: " + ((APIException) ex).isHard() + "] " + ex.getMessage());
                }
            } else {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.error(ex.getMessage(), oldEx != null ? oldEx : ex);
                } else {
                    logger.error(ex.getMessage());
                }
            }
            return null;
        }

        public @NonNull CallEvents[] callExtensionMethodsOn() { return events; }
    }
}

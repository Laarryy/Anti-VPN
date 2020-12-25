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
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerAnalyticsHook implements PluginHook {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CapabilityService capabilities;

    public PlayerAnalyticsHook(ProxyServer proxy) {
        capabilities = CapabilityService.getInstance();

        if (isCapabilityAvailable("DATA_EXTENSION_VALUES") && isCapabilityAvailable("DATA_EXTENSION_TABLES")) {
            try {
                ExtensionService.getInstance().register(new Data(proxy));
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

    private boolean isCapabilityAvailable(String capability) {
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
        private final VPNAPI api = VPNAPI.getInstance();
        private final CallEvents[] events = new CallEvents[] { CallEvents.SERVER_PERIODICAL, CallEvents.SERVER_EXTENSION_REGISTER, CallEvents.PLAYER_JOIN };

        private final ProxyServer proxy;

        private Data(ProxyServer proxy) {
            this.proxy = proxy;
        }

        @NumberProvider(
                text = "VPN Users",
                description = "Number of online VPN users.",
                priority = 2,
                iconName = "user-shield",
                iconFamily = Family.SOLID,
                iconColor = Color.NONE,
                format = FormatType.NONE
        )
        public long getVPNs() {
            Optional<CachedConfig> cachedConfig = ConfigUtil.getCachedConfig();
            if (!cachedConfig.isPresent()) {
                logger.error("Cached config could not be fetched.");
                return 0L;
            }

            long retVal = 0L;
            for (Player p : proxy.getAllPlayers()) {
                String ip = getIp(p);
                if (ip == null || ip.isEmpty()) {
                    continue;
                }

                if (cachedConfig.get().getVPNAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
                    try {
                        if (api.consensus(ip) >= cachedConfig.get().getVPNAlgorithmConsensus()) {
                            retVal++;
                        }
                    } catch (APIException ex) {
                        if (cachedConfig.get().getDebug()) {
                            logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                        } else {
                            logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                        }
                    }
                } else {
                    try {
                        if (api.cascade(ip)) {
                            retVal++;
                        }
                    } catch (APIException ex) {
                        if (cachedConfig.get().getDebug()) {
                            logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                        } else {
                            logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                        }
                    }
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
        public long getMCLeaks() {
            Optional<CachedConfig> cachedConfig = ConfigUtil.getCachedConfig();
            if (!cachedConfig.isPresent()) {
                logger.error("Cached config could not be fetched.");
                return 0L;
            }

            long retVal = 0L;
            for (Player p : proxy.getAllPlayers()) {
                try {
                    if (api.isMCLeaks(p.getUniqueId())) {
                        retVal++;
                    }
                } catch (APIException ex) {
                    if (cachedConfig.get().getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
                }
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
        public boolean getUsingVPN(UUID playerID) {
            Optional<Player> player = proxy.getPlayer(playerID);
            if (!player.isPresent()) {
                return false;
            }

            String ip = getIp(player.get());
            if (ip == null || ip.isEmpty()) {
                return false;
            }

            Optional<CachedConfig> cachedConfig = ConfigUtil.getCachedConfig();
            if (!cachedConfig.isPresent()) {
                logger.error("Cached config could not be fetched.");
                return false;
            }

            if (cachedConfig.get().getVPNAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
                try {
                    return api.consensus(ip) >= cachedConfig.get().getVPNAlgorithmConsensus();
                } catch (APIException ex) {
                    if (cachedConfig.get().getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
                }
            } else {
                try {
                    return api.cascade(ip);
                } catch (APIException ex) {
                    if (cachedConfig.get().getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
                }
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
        public boolean getMCLeaks(UUID playerID) {
            Optional<CachedConfig> cachedConfig = ConfigUtil.getCachedConfig();
            if (!cachedConfig.isPresent()) {
                logger.error("Cached config could not be fetched.");
                return false;
            }

            try {
                return api.isMCLeaks(playerID);
            } catch (APIException ex) {
                if (cachedConfig.get().getDebug()) {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                }
            }
            return false;
        }

        private String getIp(Player player) {
            InetSocketAddress address = player.getRemoteAddress();
            if (address == null) {
                return null;
            }
            InetAddress host = address.getAddress();
            if (host == null) {
                return null;
            }
            return host.getHostAddress();
        }

        public CallEvents[] callExtensionMethodsOn() { return events; }
    }
}

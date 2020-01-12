package me.egg82.antivpn.hooks;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.ExtensionService;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import me.egg82.antivpn.VPNAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerAnalyticsHook implements PluginHook {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CapabilityService capabilities;

    public PlayerAnalyticsHook() {
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

    private boolean isCapabilityAvailable(String capability) {
        try {
            return capabilities.hasCapability(capability);
        } catch (NoClassDefFoundError ignored) {
            return false;
        }
    }

    @PluginInfo(
            name = "AntiVPN",
            iconName = "shield",
            iconFamily = Family.REGULAR,
            color = Color.BLUE
    )
    class Data implements DataExtension {
        private final VPNAPI api = VPNAPI.getInstance();
        private final CallEvents[] events = new CallEvents[] { CallEvents.SERVER_PERIODICAL, CallEvents.SERVER_EXTENSION_REGISTER, CallEvents.PLAYER_JOIN };

        private Data() { }

        // TODO: Finish PLAN

        /*public InspectContainer getPlayerData(UUID uuid, InspectContainer container) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                return container;
            }

            String ip = getIp(player);
            if (ip == null || ip.isEmpty()) {
                return container;
            }

            Optional<Configuration> config = ConfigUtil.getConfig();
            if (!config.isPresent()) {
                return container;
            }

            Optional<Boolean> isVPN = Optional.empty();

            if (config.get().getNode("action", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
                double consensus = clamp(0.0d, 1.0d, config.get().getNode("action", "algorithm", "min-consensus").getDouble(0.6d));
                try {
                    isVPN = Optional.of(api.consensus(ip) >= consensus);
                } catch (APIException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } else {
                try {
                    isVPN = Optional.of(api.cascade(ip));
                } catch (APIException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }

            container.addValue("Using VPN/Proxy", (isVPN.isPresent()) ? (isVPN.get() ? "Yes" : "No") : "ERROR");

            return container;
        }

        public AnalysisContainer getServerData(Collection<UUID> uuids, AnalysisContainer container) {
            Optional<Configuration> config = ConfigUtil.getConfig();
            if (!config.isPresent()) {
                return container;
            }

            if (!config.get().getNode("action", "kick-message").getString("").isEmpty() || !config.get().getNode("action", "command").getString("").isEmpty()) {
                container.addValue("Proxies/VPNs actioned upon", AnalyticsHelper.getBlocked() + " since startup.");
            }

            int vpns = 0;
            for (UUID uuid : uuids) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }

                String ip = getIp(player);
                if (ip == null || ip.isEmpty()) {
                    continue;
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
                    vpns++;
                }
            }

            container.addValue("Proxies/VPNs in use", vpns);

            return container;
        }

        private String getIp(Player player) {
            InetSocketAddress address = player.getAddress();
            if (address == null) {
                return null;
            }
            InetAddress host = address.getAddress();
            if (host == null) {
                return null;
            }
            return host.getHostAddress();
        }*/

        public CallEvents[] callExtensionMethodsOn() { return events; }
    }
}

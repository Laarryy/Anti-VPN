package me.egg82.antivpn.hooks;

import com.djrapitops.plan.api.PlanAPI;
import com.djrapitops.plan.data.element.AnalysisContainer;
import com.djrapitops.plan.data.element.InspectContainer;
import com.djrapitops.plan.data.plugin.ContainerSize;
import com.djrapitops.plan.data.plugin.PluginData;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.djrapitops.plan.utilities.html.icon.Color;
import com.djrapitops.plan.utilities.html.icon.Icon;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.services.AnalyticsHelper;
import me.egg82.antivpn.utils.ConfigUtil;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerAnalyticsHook implements PluginHook {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PlayerAnalyticsHook(Plugin plugin) { PlanAPI.getInstance().addPluginDataSource(new Data(plugin)); }

    public void cancel() {}

    class Data extends PluginData {
        private final VPNAPI api = VPNAPI.getInstance();

        private final Plugin plugin;

        private Data(Plugin plugin) {
            super(ContainerSize.THIRD, "Anti-VPN");
            setPluginIcon(Icon.called("ban").of(Color.RED).build());

            this.plugin = plugin;
        }

        public InspectContainer getPlayerData(UUID uuid, InspectContainer container) {
            ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
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

            if (config.get().getNode("kick", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
                double consensus = clamp(0.0d, 1.0d, config.get().getNode("kick", "algorithm", "min-consensus").getDouble(0.6d));
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

            if (config.get().getNode("kick", "enabled").getBoolean(true)) {
                container.addValue("Proxies/VPNs blocked", AnalyticsHelper.getBlocked() + " since startup.");
            }

            int vpns = 0;
            for (UUID uuid : uuids) {
                ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
                if (player == null) {
                    continue;
                }

                String ip = getIp(player);
                if (ip == null || ip.isEmpty()) {
                    continue;
                }

                boolean isVPN;

                if (config.get().getNode("kick", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
                    double consensus = clamp(0.0d, 1.0d, config.get().getNode("kick", "algorithm", "min-consensus").getDouble(0.6d));
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

        private String getIp(ProxiedPlayer player) {
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

        private double clamp(double min, double max, double val) { return Math.min(max, Math.max(min, val)); }
    }
}

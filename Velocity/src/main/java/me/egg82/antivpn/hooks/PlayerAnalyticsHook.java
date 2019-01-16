package me.egg82.antivpn.hooks;

import com.djrapitops.plan.api.PlanAPI;
import com.djrapitops.plan.data.element.AnalysisContainer;
import com.djrapitops.plan.data.element.InspectContainer;
import com.djrapitops.plan.data.plugin.ContainerSize;
import com.djrapitops.plan.data.plugin.PluginData;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.services.AnalyticsHelper;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerAnalyticsHook implements PluginHook {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PlayerAnalyticsHook(ProxyServer proxy) { PlanAPI.getInstance().addPluginDataSource(new Data(proxy)); }

    public void cancel() {}

    class Data extends PluginData {
        private final VPNAPI api = VPNAPI.getInstance();

        private final ProxyServer proxy;

        private Data(ProxyServer proxy) {
            super(ContainerSize.THIRD, "Anti-VPN");
            setPluginIcon("ban");
            setIconColor("red");

            this.proxy = proxy;
        }

        public InspectContainer getPlayerData(UUID uuid, InspectContainer container) {
            Optional<Player> player = proxy.getPlayer(uuid);
            if (!player.isPresent()) {
                return container;
            }

            String ip = getIp(player.get());
            if (ip == null || ip.isEmpty()) {
                return container;
            }

            Configuration config;

            try {
                config = ServiceLocator.get(Configuration.class);
            } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
                logger.error(ex.getMessage(), ex);
                return container;
            }

            boolean isVPN;

            if (config.getNode("kick", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
                double consensus = clamp(0.0d, 1.0d, config.getNode("kick", "algorithm", "min-consensus").getDouble(0.6d));
                isVPN = api.consensus(ip) >= consensus;
            } else {
                isVPN = api.cascade(ip);
            }

            container.addValue("Using VPN/Proxy", (isVPN) ? "Yes" : "No");

            return container;
        }

        public AnalysisContainer getServerData(Collection<UUID> uuids, AnalysisContainer container) {
            Configuration config;

            try {
                config = ServiceLocator.get(Configuration.class);
            } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
                logger.error(ex.getMessage(), ex);
                return container;
            }

            if (config.getNode("kick", "enabled").getBoolean(true)) {
                container.addValue("Proxies/VPNs blocked", AnalyticsHelper.getBlocked() + " since startup.");
            }

            int vpns = 0;
            for (UUID uuid : uuids) {
                Optional<Player> player = proxy.getPlayer(uuid);
                if (!player.isPresent()) {
                    continue;
                }

                String ip = getIp(player.get());
                if (ip == null || ip.isEmpty()) {
                    continue;
                }

                boolean isVPN;

                if (config.getNode("kick", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
                    double consensus = clamp(0.0d, 1.0d, config.getNode("kick", "algorithm", "min-consensus").getDouble(0.6d));
                    isVPN = api.consensus(ip) >= consensus;
                } else {
                    isVPN = api.cascade(ip);
                }

                if (isVPN) {
                    vpns++;
                }
            }

            container.addValue("Proxies/VPNs in use", vpns);

            return container;
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

        private double clamp(double min, double max, double val) { return Math.min(max, Math.max(min, val)); }
    }
}

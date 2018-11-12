package me.egg82.antivpn.hooks;

import com.djrapitops.plan.api.PlanAPI;
import com.djrapitops.plan.data.element.AnalysisContainer;
import com.djrapitops.plan.data.element.InspectContainer;
import com.djrapitops.plan.data.plugin.ContainerSize;
import com.djrapitops.plan.data.plugin.PluginData;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.extended.Configuration;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerAnalyticsHook implements PluginHook {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final AtomicLong blocked = new AtomicLong(0L);

    public PlayerAnalyticsHook(Plugin plugin) { PlanAPI.getInstance().addPluginDataSource(new Data(plugin)); }

    public void cancel() {}

    public static void incrementBlocked() { blocked.getAndIncrement(); }

    public static long getBlocked() { return blocked.get(); }

    class Data extends PluginData {
        private final VPNAPI api = VPNAPI.getInstance();

        private final Plugin plugin;

        private Data(Plugin plugin) {
            super(ContainerSize.THIRD, "Anti-VPN");
            setPluginIcon("ban");
            setIconColor("red");

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
                container.addValue("Proxies/VPNs blocked", blocked.get() + " since startup.");
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

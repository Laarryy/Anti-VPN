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
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.services.AnalyticsHelper;
import me.egg82.antivpn.utils.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerAnalyticsHook implements PluginHook {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PlayerAnalyticsHook() { PlanAPI.getInstance().addPluginDataSource(new Data()); }

    public void cancel() {}

    class Data extends PluginData {
        private final VPNAPI api = VPNAPI.getInstance();

        private Data() {
            super(ContainerSize.THIRD, "Anti-VPN");
            setPluginIcon("ban");
            setIconColor("red");
        }

        public InspectContainer getPlayerData(UUID uuid, InspectContainer container) {
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

            boolean isVPN;

            if (config.get().getNode("kick", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
                double consensus = clamp(0.0d, 1.0d, config.get().getNode("kick", "algorithm", "min-consensus").getDouble(0.6d));
                isVPN = api.consensus(ip) >= consensus;
            } else {
                isVPN = api.cascade(ip);
            }

            container.addValue("Using VPN/Proxy", (isVPN) ? "Yes" : "No");

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
                Player player = Bukkit.getPlayer(uuid);
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

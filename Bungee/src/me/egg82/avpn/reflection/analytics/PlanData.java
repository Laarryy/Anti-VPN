package me.egg82.avpn.reflection.analytics;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;

import com.djrapitops.plan.data.element.AnalysisContainer;
import com.djrapitops.plan.data.element.InspectContainer;
import com.djrapitops.plan.data.plugin.ContainerSize;
import com.djrapitops.plan.data.plugin.PluginData;

import me.egg82.avpn.AnalyticsData;
import me.egg82.avpn.Config;
import me.egg82.avpn.VPNAPI;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ninja.egg82.bungeecord.utils.CommandUtil;

public class PlanData extends PluginData {
    // vars
    private VPNAPI api = VPNAPI.getInstance();

    // constructor
    public PlanData() {
        super(ContainerSize.THIRD, "Anti-VPN");
        this.setPluginIcon("ban");
        this.setIconColor("red");
    }

    // public
    public InspectContainer getPlayerData(UUID uuid, InspectContainer container) {
        ProxiedPlayer player = CommandUtil.getPlayerByUuid(uuid);
        if (player == null) {
            return container;
        }

        String ip = getIp(player);
        if (ip == null || ip.isEmpty()) {
            return container;
        }

        boolean isVPN = api.isVPN(ip, true);
        container.addValue("Using VPN/Proxy", (isVPN) ? "Yes" : "No");

        return container;
    }
    public AnalysisContainer getServerData(Collection<UUID> uuids, AnalysisContainer container) {
        if (Config.kick) {
            container.addValue("Proxies/VPNs blocked", AnalyticsData.playersBlocked + " since startup");
        }

        int vpns = 0;
        for (UUID uuid : uuids) {
            ProxiedPlayer player = CommandUtil.getPlayerByUuid(uuid);
            if (player == null) {
                return container;
            }

            String ip = getIp(player);
            if (ip == null || ip.isEmpty()) {
                return container;
            }

            if (api.isVPN(ip)) {
                vpns++;
            }
        }

        container.addValue("Proxies/VPNs in use", Integer.valueOf(vpns));
        
        return container;
    }

    // private
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
}

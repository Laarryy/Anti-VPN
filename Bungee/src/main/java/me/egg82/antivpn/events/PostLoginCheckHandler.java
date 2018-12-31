package me.egg82.antivpn.events;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.function.Consumer;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.hooks.PlayerAnalyticsHook;
import me.egg82.antivpn.utils.LogUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PostLoginEvent;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostLoginCheckHandler implements Consumer<PostLoginEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final VPNAPI api = VPNAPI.getInstance();

    public void accept(PostLoginEvent event) {
        String ip = getIp(event.getPlayer().getAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        Configuration config;
        CachedConfigValues cachedConfig;

        try {
            config = ServiceLocator.get(Configuration.class);
            cachedConfig = ServiceLocator.get(CachedConfigValues.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (event.getPlayer().hasPermission("avpn.bypass")) {
            if (cachedConfig.getDebug()) {
                logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.YELLOW + " bypasses check. Ignoring.");
            }
            return;
        }

        if (!config.getNode("kick", "enabled").getBoolean(true)) {
            if (cachedConfig.getDebug()) {
                logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "Plugin set to API-only. Ignoring " + ChatColor.WHITE + event.getPlayer().getName());
            }
            return;
        }

        if (cachedConfig.getIgnoredIps().contains(ip)) {
            return;
        }

        boolean isVPN;

        if (config.getNode("kick", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
            double consensus = clamp(0.0d, 1.0d, config.getNode("kick", "algorithm", "min-consensus").getDouble(0.6d));
            isVPN = api.consensus(ip) >= consensus;
        } else {
            isVPN = api.cascade(ip);
        }

        if (isVPN) {
            PlayerAnalyticsHook.incrementBlocked();
            if (cachedConfig.getDebug()) {
                logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.DARK_RED + " found using a VPN. Kicking with defined message.");
            }

            event.getPlayer().disconnect(new TextComponent(config.getNode("kick", "message").getString("")));
        } else {
            logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getPlayer().getName() + ChatColor.GREEN + " passed VPN check.");
        }
    }

    private String getIp(InetSocketAddress address) {
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

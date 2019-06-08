package me.egg82.antivpn.events;

import java.net.InetAddress;
import java.util.Optional;
import java.util.function.Consumer;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.LogUtil;
import org.bukkit.ChatColor;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncPlayerPreLoginCacheHandler implements Consumer<AsyncPlayerPreLoginEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final VPNAPI api = VPNAPI.getInstance();

    public void accept(AsyncPlayerPreLoginEvent event) {
        // Basically this event is here to cache the result of the IP check before
        // PlayerJoin gets ahold of it
        String ip = getIp(event.getAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        Optional<Configuration> config = ConfigUtil.getConfig();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!config.isPresent() || !cachedConfig.isPresent()) {
            return;
        }

        if (cachedConfig.get().getIgnoredIps().contains(ip)) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + ChatColor.WHITE + event.getUniqueId() + ChatColor.YELLOW + " is using an ignored IP " + ChatColor.WHITE + ip +  ChatColor.YELLOW + ". Ignoring.");
            }
            return;
        }

        if (config.get().getNode("kick", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
            api.consensus(ip); // Calling this will cache the result internally, even if the value is unused
        } else {
            api.cascade(ip); // Calling this will cache the result internally, even if the value is unused
        }
    }

    private String getIp(InetAddress address) {
        if (address == null) {
            return null;
        }

        return address.getHostAddress();
    }
}

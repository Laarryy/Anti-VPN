package me.egg82.avpn.events;

import java.net.InetAddress;
import java.util.UUID;

import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import me.egg82.avpn.Config;
import me.egg82.avpn.VPNAPI;
import me.egg82.avpn.registries.UUIDIPRegistry;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.registries.IRegistry;
import ninja.egg82.plugin.handlers.events.EventHandler;

public class AsyncPlayerPreLoginIPCache extends EventHandler<AsyncPlayerPreLoginEvent> {
    // vars
    private IRegistry<UUID, String> uuidIpRegistry = ServiceLocator.getService(UUIDIPRegistry.class);

    private VPNAPI api = VPNAPI.getInstance();

    // constructor
    public AsyncPlayerPreLoginIPCache() {

    }

    // public

    // private
    protected void onExecute(long elapsedMilliseconds) {
        // Basically this event is here to cache the result of the IP check before
        // PlayerJoin gets ahold of it

        String ip = getIp(event.getAddress());

        if (ip == null || ip.isEmpty()) {
            return;
        }

        uuidIpRegistry.setRegister(event.getUniqueId(), ip);

        if (Config.ignore.contains(ip)) {
            return;
        }

        if (Config.consensus >= 0.0d) {
            // Consensus algorithm
            api.consensus(ip, true); // Calling this will cache the result internally, even if the value is unused
        } else {
            // Cascade algorithm
            api.isVPN(ip, true); // Calling this will cache the result internally, even if the value is unused
        }
    }

    private String getIp(InetAddress address) {
        if (address == null) {
            return null;
        }

        return address.getHostAddress();
    }
}

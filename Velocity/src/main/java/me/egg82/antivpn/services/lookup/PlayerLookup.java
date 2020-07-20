package me.egg82.antivpn.services.lookup;

import com.velocitypowered.api.proxy.ProxyServer;

import java.io.IOException;
import java.util.UUID;

public class PlayerLookup {
    private PlayerLookup() { }

    public static PlayerInfo get(UUID uuid, ProxyServer proxy) throws IOException {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid cannot be null.");
        }
        if (proxy == null) {
            throw new IllegalArgumentException("proxy cannot be null.");
        }

        return new VelocityPlayerInfo(uuid, proxy);
    }

    public static PlayerInfo get(String name, ProxyServer proxy) throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }
        if (proxy == null) {
            throw new IllegalArgumentException("proxy cannot be null.");
        }

        return new VelocityPlayerInfo(name, proxy);
    }
}

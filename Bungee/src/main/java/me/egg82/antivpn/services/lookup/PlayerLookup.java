package me.egg82.antivpn.services.lookup;

import java.io.IOException;
import java.util.UUID;

public class PlayerLookup {
    private PlayerLookup() { }

    public static PlayerInfo get(UUID uuid) throws IOException {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid cannot be null.");
        }

        return new BungeePlayerInfo(uuid);
    }

    public static PlayerInfo get(String name) throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }

        return new BungeePlayerInfo(name);
    }
}

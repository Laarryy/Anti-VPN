package me.egg82.antivpn.services.lookup;

import java.io.IOException;
import java.util.UUID;

public class PlayerLookup {
    private PlayerLookup() {}

    private static boolean isPaper = true;

    static {
        try {
            Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
        } catch (ClassNotFoundException ignored) {
            isPaper = false;
        }
    }

    public static PlayerInfo get(UUID uuid) throws IOException {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid cannot be null.");
        }

        return (isPaper) ? new PaperPlayerInfo(uuid) : new BukkitPlayerInfo(uuid);
    }

    public static PlayerInfo get(String name) throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }

        return (isPaper) ? new PaperPlayerInfo(name) : new BukkitPlayerInfo(name);
    }
}

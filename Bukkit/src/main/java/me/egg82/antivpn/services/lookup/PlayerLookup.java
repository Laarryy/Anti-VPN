package me.egg82.antivpn.services.lookup;

import java.io.IOException;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PlayerLookup {
    private PlayerLookup() { }

    private static final boolean IS_PAPER;

    static {
        boolean paper = true;
        try {
            Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
        } catch (ClassNotFoundException ignored) {
            paper = false;
        }
        IS_PAPER = paper;
    }

    public static @NonNull PlayerInfo get(@NonNull UUID uuid) throws IOException { return (IS_PAPER) ? new PaperPlayerInfo(uuid) : new BukkitPlayerInfo(uuid); }

    public static @NonNull PlayerInfo get(@NonNull String name) throws IOException { return (IS_PAPER) ? new PaperPlayerInfo(name) : new BukkitPlayerInfo(name); }
}

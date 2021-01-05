package me.egg82.antivpn.services.lookup;

import java.io.IOException;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PlayerLookup {
    private PlayerLookup() { }

    public static @NonNull PlayerInfo get(@NonNull UUID uuid) throws IOException { return new BungeePlayerInfo(uuid); }

    public static @NonNull PlayerInfo get(@NonNull String name) throws IOException { return new BungeePlayerInfo(name); }
}

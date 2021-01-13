package me.egg82.antivpn.services.lookup;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.util.UUID;

public class PlayerLookup {
    private PlayerLookup() { }

    public static @NonNull PlayerInfo get(@NonNull UUID uuid) throws IOException { return new VelocityPlayerInfo(uuid); }

    public static @NonNull PlayerInfo get(@NonNull String name) throws IOException { return new VelocityPlayerInfo(name); }
}

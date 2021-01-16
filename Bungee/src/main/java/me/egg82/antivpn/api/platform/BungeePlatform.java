package me.egg82.antivpn.api.platform;

import com.google.common.collect.ImmutableSet;
import java.net.InetAddress;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BungeePlatform implements Platform {
    private static final Set<UUID> uniquePlayers = new HashSet<>();
    private static final Set<InetAddress> uniqueIps = new HashSet<>();

    public static void addUniquePlayer(@NonNull UUID uuid) { uniquePlayers.add(uuid); }

    public static void addUniqueIp(@NonNull InetAddress ip) { uniqueIps.add(ip); }

    private final Instant startTime;

    public BungeePlatform(long startTime) {
        this.startTime = Instant.ofEpochMilli(startTime);
    }

    public @NonNull Type getType() { return Type.BUNGEECORD; }

    public @NonNull Set<UUID> getUniquePlayers() { return ImmutableSet.copyOf(uniquePlayers); }

    public @NonNull Set<InetAddress> getUniqueIPs() { return ImmutableSet.copyOf(uniqueIps); }

    public @NonNull Instant getStartTime() { return startTime; }
}

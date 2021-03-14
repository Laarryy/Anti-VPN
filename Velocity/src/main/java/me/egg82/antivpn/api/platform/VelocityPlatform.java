package me.egg82.antivpn.api.platform;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VelocityPlatform implements Platform {
    private static final Set<UUID> uniquePlayers = new HashSet<>();
    private static final Set<InetAddress> uniqueIps = new HashSet<>();

    public static void addUniquePlayer(@NotNull UUID uuid) {
        uniquePlayers.add(uuid);
    }

    public static void addUniqueIp(@NotNull InetAddress ip) {
        uniqueIps.add(ip);
    }

    private final Instant startTime;

    public VelocityPlatform(long startTime) {
        this.startTime = Instant.ofEpochMilli(startTime);
    }

    @Override
    public @NotNull Type getType() { return Type.VELOCITY; }

    @Override
    public @NotNull Set<UUID> getUniquePlayers() { return ImmutableSet.copyOf(uniquePlayers); }

    @Override
    public @NotNull Set<InetAddress> getUniqueIPs() { return ImmutableSet.copyOf(uniqueIps); }

    @Override
    public @NotNull Instant getStartTime() { return startTime; }
}

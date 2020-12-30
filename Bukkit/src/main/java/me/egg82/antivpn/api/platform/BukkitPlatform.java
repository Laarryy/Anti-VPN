package me.egg82.antivpn.api.platform;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BukkitPlatform implements Platform {
    private final long startTime;

    public BukkitPlatform(long startTime) {
        this.startTime = startTime;
    }

    public @NonNull Type getType() { return Type.BUKKIT; }

    public @NonNull Set<UUID> getUniquePlayers() {

    }

    public @NonNull Set<String> getUniqueIps() {

    }

    public @NonNull Instant getStartTime() { return Instant.ofEpochMilli(startTime); }
}

package me.egg82.antivpn.api.model.player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BukkitPlayerManager extends AbstractPlayerManager {
    public @Nullable CompletableFuture<Player> getPlayer(@NonNull UUID uniqueId) {

    }

    public @Nullable CompletableFuture<Player> getPlayer(@NonNull String username) {

    }
}

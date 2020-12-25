package me.egg82.antivpn.api.model.player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractPlayerManager implements PlayerManager {
    public @NonNull CompletableFuture<Void> savePlayer(@NonNull Player player) {

    }

    public @NonNull CompletableFuture<Void> deletePlayer(@NonNull UUID uniqueId) {

    }

    public @NonNull CompletableFuture<Set<UUID>> getPlayers() {

    }

    public @NonNull CompletableFuture<Boolean> checkMcLeaks(@NonNull UUID uniqueId, boolean useCache) throws APIException {

    }
}

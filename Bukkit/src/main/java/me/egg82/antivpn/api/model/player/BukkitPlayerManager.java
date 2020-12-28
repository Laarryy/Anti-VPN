package me.egg82.antivpn.api.model.player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.storage.models.PlayerModel;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BukkitPlayerManager extends AbstractPlayerManager {
    public @NonNull CompletableFuture<Player> getPlayer(@NonNull UUID uniqueId) {

    }

    public @NonNull CompletableFuture<Player> getPlayer(@NonNull String username) {

    }

    protected @NonNull PlayerModel calculatePlayerResult(@NonNull UUID uuid, boolean useCache) throws APIException {

    }
}

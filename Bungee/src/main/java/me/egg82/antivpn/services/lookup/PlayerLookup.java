package me.egg82.antivpn.services.lookup;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PlayerLookup {
    private PlayerLookup() { }

    public static @NonNull CompletableFuture<PlayerInfo> get(@NonNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new BungeePlayerInfo(uuid);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        });
    }

    public static @NonNull CompletableFuture<PlayerInfo> get(@NonNull String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new BungeePlayerInfo(name);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        });
    }
}

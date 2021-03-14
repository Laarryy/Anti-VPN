package me.egg82.antivpn.services.lookup;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class PlayerLookup {
    private PlayerLookup() {
    }

    public static @NotNull CompletableFuture<PlayerInfo> get(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new BungeePlayerInfo(uuid);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        });
    }

    public static @NotNull CompletableFuture<PlayerInfo> get(@NotNull String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new BungeePlayerInfo(name);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        });
    }
}

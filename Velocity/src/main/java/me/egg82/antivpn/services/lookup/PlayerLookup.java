package me.egg82.antivpn.services.lookup;

import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class PlayerLookup {
    private PlayerLookup() { }

    public static @NotNull CompletableFuture<PlayerInfo> get(@NotNull UUID uuid, @NotNull ProxyServer proxy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new VelocityPlayerInfo(uuid, proxy);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        });
    }

    public static @NotNull CompletableFuture<PlayerInfo> get(@NotNull String name, @NotNull ProxyServer proxy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new VelocityPlayerInfo(name, proxy);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        });
    }
}

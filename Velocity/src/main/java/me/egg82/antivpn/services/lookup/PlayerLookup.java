package me.egg82.antivpn.services.lookup;

import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PlayerLookup {
    private PlayerLookup() { }

    public static @NonNull CompletableFuture<PlayerInfo> get(@NonNull UUID uuid, @NonNull ProxyServer proxy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new VelocityPlayerInfo(uuid, proxy);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        });
    }

    public static @NonNull CompletableFuture<PlayerInfo> get(@NonNull String name, @NonNull ProxyServer proxy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new VelocityPlayerInfo(name, proxy);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        });
    }
}

package me.egg82.antivpn.services.lookup;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PlayerLookup {
    private PlayerLookup() { }

    private static final boolean IS_PAPER;

    static {
        boolean paper = true;
        try {
            Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
        } catch (ClassNotFoundException ignored) {
            paper = false;
        }
        IS_PAPER = paper;
    }

    public static @NonNull CompletableFuture<PlayerInfo> get(@NonNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return IS_PAPER ? new PaperPlayerInfo(uuid) : new BukkitPlayerInfo(uuid);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        });
    }

    public static @NonNull CompletableFuture<PlayerInfo> get(@NonNull String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return IS_PAPER ? new PaperPlayerInfo(name) : new BukkitPlayerInfo(name);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        });
    }
}

package me.egg82.antivpn.services.lookup;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class PlayerLookup {
    private PlayerLookup() {
    }

    private static final boolean HAS_PLAYERPROFILE_API;

    static {
        boolean api = true;
        try {
            Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
        } catch (ClassNotFoundException ignored) {
            api = false;
        }
        HAS_PLAYERPROFILE_API = api;
    }

    public static @NotNull CompletableFuture<@NotNull PlayerInfo> get(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return HAS_PLAYERPROFILE_API ? new PaperPlayerInfo(uuid) : new BukkitPlayerInfo(uuid);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        });
    }

    public static @NotNull CompletableFuture<@NotNull PlayerInfo> get(@NotNull String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return HAS_PLAYERPROFILE_API ? new PaperPlayerInfo(name) : new BukkitPlayerInfo(name);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        });
    }
}

package me.egg82.antivpn.api.model.player;

import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitPlayer extends AbstractPlayer {
    public BukkitPlayer(@NotNull UUID uuid, boolean mcleaks) {
        this(uuid, null, mcleaks);
    }

    public BukkitPlayer(@NotNull UUID uuid, @Nullable String name, boolean mcleaks) {
        super(uuid, name, mcleaks);
    }

    @Override
    protected @NotNull CompletableFuture<@NotNull String> fetchName(@NotNull UUID uuid) { return PlayerLookup.get(uuid).thenApply(PlayerInfo::getName); }
}

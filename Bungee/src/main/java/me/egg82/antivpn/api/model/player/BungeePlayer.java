package me.egg82.antivpn.api.model.player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import org.jetbrains.annotations.NotNull;

public class BungeePlayer extends AbstractPlayer {
    public BungeePlayer(@NotNull UUID uuid, boolean mcleaks) { this(uuid, null, mcleaks); }

    public BungeePlayer(@NotNull UUID uuid, String name, boolean mcleaks) { super(uuid, name, mcleaks); }

    protected @NotNull CompletableFuture<String> fetchName(@NotNull UUID uuid) { return PlayerLookup.get(uuid).thenApply(PlayerInfo::getName); }
}

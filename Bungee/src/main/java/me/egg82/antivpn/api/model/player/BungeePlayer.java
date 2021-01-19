package me.egg82.antivpn.api.model.player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BungeePlayer extends AbstractPlayer {
    public BungeePlayer(@NonNull UUID uuid, boolean mcleaks) { this(uuid, null, mcleaks); }

    public BungeePlayer(@NonNull UUID uuid, String name, boolean mcleaks) { super(uuid, name, mcleaks); }

    protected @NonNull CompletableFuture<String> fetchName(@NonNull UUID uuid) { return PlayerLookup.get(uuid).thenApply(PlayerInfo::getName); }
}

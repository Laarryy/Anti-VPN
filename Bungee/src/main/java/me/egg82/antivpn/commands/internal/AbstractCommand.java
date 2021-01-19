package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCommand implements Runnable {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final CommandIssuer issuer;

    protected AbstractCommand(@NonNull CommandIssuer issuer) {
        this.issuer = issuer;
    }

    protected @NonNull CompletableFuture<UUID> fetchUuid(@NonNull String name) { return PlayerLookup.get(name).thenApply(PlayerInfo::getUUID); }
}

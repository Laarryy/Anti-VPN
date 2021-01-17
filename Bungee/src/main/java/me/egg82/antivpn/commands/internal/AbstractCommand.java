package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import java.io.IOException;
import java.util.UUID;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCommand implements Runnable {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final CommandIssuer issuer;

    protected AbstractCommand(@NonNull CommandIssuer issuer) {
        this.issuer = issuer;
    }

    protected @Nullable UUID fetchUuid(@NonNull String name) {
        PlayerInfo info;
        try {
            info = PlayerLookup.get(name);
        } catch (IOException ex) {
            logger.warn("Could not fetch player UUID. (rate-limited?)", ex);
            return null;
        }
        return info.getUUID();
    }
}

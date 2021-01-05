package me.egg82.antivpn.api.model.player;

import java.io.IOException;
import java.util.UUID;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BungeePlayer extends AbstractPlayer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public BungeePlayer(@NonNull UUID uuid, boolean mcleaks) { this(uuid, null, mcleaks); }

    public BungeePlayer(@NonNull UUID uuid, String name, boolean mcleaks) { super(uuid, name, mcleaks); }

    protected @Nullable String fetchName(@NonNull UUID uuid) {
        PlayerInfo info;
        try {
            info = PlayerLookup.get(uuid);
        } catch (IOException ex) {
            logger.warn("Could not fetch player name. (rate-limited?)", ex);
            return null;
        }
        return info.getName();
    }
}

package me.egg82.antivpn.api.model.player;

import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public class VelocityPlayer extends AbstractPlayer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProxyServer proxy;

    public VelocityPlayer(@NonNull ProxyServer proxy, @NonNull UUID uuid, boolean mcleaks) { this(proxy, uuid, null, mcleaks); }

    public VelocityPlayer(@NonNull ProxyServer proxy, @NonNull UUID uuid, String name, boolean mcleaks) {
        super(uuid, name, mcleaks);
        this.proxy = proxy;
    }

    protected @Nullable String fetchName(@NonNull UUID uuid) {
        PlayerInfo info;
        try {
            info = PlayerLookup.get(uuid, proxy);
        } catch (IOException ex) {
            logger.warn("Could not fetch player name. (rate-limited?)", ex);
            return null;
        }
        return info.getName();
    }
}

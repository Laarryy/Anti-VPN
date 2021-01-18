package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.util.UUID;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCommand implements Runnable {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final ProxyServer proxy;
    protected final CommandIssuer issuer;

    protected AbstractCommand(@NonNull ProxyServer proxy, @NonNull CommandIssuer issuer) {
        this.proxy = proxy;
        this.issuer = issuer;
    }

    protected @Nullable UUID fetchUuid(@NonNull String name) {
        PlayerInfo info;
        try {
            info = PlayerLookup.get(name, proxy);
        } catch (IOException ex) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.warn("Could not fetch player UUID. (rate-limited?)", ex);
            } else {
                logger.warn("Could not fetch player UUID. (rate-limited?)");
            }
            return null;
        }
        return info.getUUID();
    }
}

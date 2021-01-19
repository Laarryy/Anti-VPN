package me.egg82.antivpn.api.model.player;

import com.velocitypowered.api.proxy.ProxyServer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import org.checkerframework.checker.nullness.qual.NonNull;

public class VelocityPlayer extends AbstractPlayer {
    private final ProxyServer proxy;

    public VelocityPlayer(@NonNull ProxyServer proxy, @NonNull UUID uuid, boolean mcleaks) { this(proxy, uuid, null, mcleaks); }

    public VelocityPlayer(@NonNull ProxyServer proxy, @NonNull UUID uuid, String name, boolean mcleaks) {
        super(uuid, name, mcleaks);
        this.proxy = proxy;
    }

    protected @NonNull CompletableFuture<String> fetchName(@NonNull UUID uuid) { return PlayerLookup.get(uuid, proxy).thenApply(PlayerInfo::getName); }
}

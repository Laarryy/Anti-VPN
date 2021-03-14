package me.egg82.antivpn.api.model.player;

import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VelocityPlayer extends AbstractPlayer {
    private final ProxyServer proxy;

    public VelocityPlayer(@NotNull ProxyServer proxy, @NotNull UUID uuid, boolean mcleaks) {
        this(proxy, uuid, null, mcleaks);
    }

    public VelocityPlayer(@NotNull ProxyServer proxy, @NotNull UUID uuid, String name, boolean mcleaks) {
        super(uuid, name, mcleaks);
        this.proxy = proxy;
    }

    @Override
    protected @NotNull CompletableFuture<String> fetchName(@NotNull UUID uuid) { return PlayerLookup.get(uuid, proxy).thenApply(PlayerInfo::getName); }
}

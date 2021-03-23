package me.egg82.antivpn.api.event.api;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.AbstractEvent;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.api.model.source.SourceManager;
import org.jetbrains.annotations.NotNull;

public class APIReloadEventImpl extends AbstractEvent implements APIReloadEvent {
    private final @NotNull IPManager newIpManager;
    private final @NotNull PlayerManager newPlayerManager;
    private final @NotNull SourceManager newSourceManager;

    public APIReloadEventImpl(@NotNull VPNAPI api, @NotNull IPManager newIpManager, @NotNull PlayerManager newPlayerManager, @NotNull SourceManager newSourceManager) {
        super(api);
        this.newIpManager = newIpManager;
        this.newPlayerManager = newPlayerManager;
        this.newSourceManager = newSourceManager;
    }

    @Override
    @NotNull
    public IPManager getNewIPManager() { return newIpManager; }

    @Override
    @NotNull
    public PlayerManager getNewPlayerManager() { return newPlayerManager; }

    @Override
    @NotNull
    public SourceManager getNewSourceManager() { return newSourceManager; }

    @Override
    public String toString() {
        return "APIReloadEventImpl{" +
                "api=" + api +
                ", newIpManager=" + newIpManager +
                ", newPlayerManager=" + newPlayerManager +
                ", newSourceManager=" + newSourceManager +
                '}';
    }
}

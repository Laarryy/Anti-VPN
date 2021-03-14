package me.egg82.antivpn.api.event.api;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.AbstractEvent;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.api.model.source.SourceManager;
import org.jetbrains.annotations.NotNull;

public class APIReloadEventImpl extends AbstractEvent implements APIReloadEvent {
    private final IPManager newIpManager;
    private final PlayerManager newPlayerManager;
    private final SourceManager newSourceManager;

    public APIReloadEventImpl(@NotNull VPNAPI api, @NotNull IPManager newIpManager, @NotNull PlayerManager newPlayerManager, @NotNull SourceManager newSourceManager) {
        super(api);
        this.newIpManager = newIpManager;
        this.newPlayerManager = newPlayerManager;
        this.newSourceManager = newSourceManager;
    }

    @Override
    public @NotNull IPManager getNewIPManager() {
        return newIpManager;
    }

    @Override
    public @NotNull PlayerManager getNewPlayerManager() {
        return newPlayerManager;
    }

    @Override
    public @NotNull SourceManager getNewSourceManager() {
        return newSourceManager;
    }

    public String toString() {
        return "APIReloadEventImpl{" +
                "api=" + api +
                ", newIpManager=" + newIpManager +
                ", newPlayerManager=" + newPlayerManager +
                ", newSourceManager=" + newSourceManager +
                '}';
    }
}

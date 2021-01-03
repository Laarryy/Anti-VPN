package me.egg82.antivpn.api.event.api;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.AbstractEvent;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.api.model.source.SourceManager;
import org.checkerframework.checker.nullness.qual.NonNull;

public class GenericAPIReloadEvent extends AbstractEvent implements APIReloadEvent {
    private final IPManager newIpManager;
    private final PlayerManager newPlayerManager;
    private final SourceManager newSourceManager;

    public GenericAPIReloadEvent(@NonNull VPNAPI api, @NonNull IPManager newIpManager, @NonNull PlayerManager newPlayerManager, @NonNull SourceManager newSourceManager) {
        super(api);
        this.newIpManager = newIpManager;
        this.newPlayerManager = newPlayerManager;
        this.newSourceManager = newSourceManager;
    }

    public @NonNull IPManager getNewIpManager() { return newIpManager; }

    public @NonNull PlayerManager getNewPlayerManager() { return newPlayerManager; }

    public @NonNull SourceManager getNewSourceManager() { return newSourceManager; }
}

package me.egg82.antivpn.api.event;

import me.egg82.antivpn.api.VPNAPI;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractEvent implements VPNEvent {
    protected final VPNAPI api;
    private final Class<? extends VPNEvent> clazz;

    protected AbstractEvent(@NotNull VPNAPI api) {
        this.api = api;
        this.clazz = getClass();
    }

    @Override
    public @NotNull VPNAPI getApi() { return api; }

    @Override
    public @NotNull Class<? extends VPNEvent> getEventType() { return clazz; }
}

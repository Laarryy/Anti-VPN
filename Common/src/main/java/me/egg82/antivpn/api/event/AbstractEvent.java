package me.egg82.antivpn.api.event;

import me.egg82.antivpn.api.VPNAPI;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractEvent implements VPNEvent {
    protected final @NotNull VPNAPI api;
    private final @NotNull Class<VPNEvent> clazz;

    @SuppressWarnings("unchecked")
    protected AbstractEvent(@NotNull VPNAPI api) {
        this.api = api;
        this.clazz = (Class<VPNEvent>) getClass();
    }

    @Override
    public @NotNull VPNAPI getApi() { return api; }

    @Override
    public @NotNull Class<VPNEvent> getEventType() { return clazz; }
}

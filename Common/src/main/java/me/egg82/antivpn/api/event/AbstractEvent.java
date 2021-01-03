package me.egg82.antivpn.api.event;

import me.egg82.antivpn.api.VPNAPI;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractEvent implements VPNEvent {
    protected final VPNAPI api;
    private final Class<? extends VPNEvent> clazz;

    protected AbstractEvent(@NonNull VPNAPI api) {
        this.api = api;
        this.clazz = getClass();
    }

    public @NonNull VPNAPI getApi() { return api; }

    public @NonNull Class<? extends VPNEvent> getEventType() { return clazz; }
}

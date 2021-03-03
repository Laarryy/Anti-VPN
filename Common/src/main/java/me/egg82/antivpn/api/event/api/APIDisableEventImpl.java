package me.egg82.antivpn.api.event.api;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.AbstractEvent;
import org.jetbrains.annotations.NotNull;

public class APIDisableEventImpl extends AbstractEvent implements APIDisableEvent {
    public APIDisableEventImpl(@NotNull VPNAPI api) {
        super(api);
    }

    public String toString() {
        return "APIDisableEventImpl{" +
            "api=" + api +
            '}';
    }
}

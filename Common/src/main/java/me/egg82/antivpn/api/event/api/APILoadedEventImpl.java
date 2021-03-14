package me.egg82.antivpn.api.event.api;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.AbstractEvent;
import org.jetbrains.annotations.NotNull;

public class APILoadedEventImpl extends AbstractEvent implements APILoadedEvent {
    public APILoadedEventImpl(@NotNull VPNAPI api) {
        super(api);
    }

    @Override
    public String toString() {
        return "APILoadedEventImpl{" +
                "api=" + api +
                '}';
    }
}

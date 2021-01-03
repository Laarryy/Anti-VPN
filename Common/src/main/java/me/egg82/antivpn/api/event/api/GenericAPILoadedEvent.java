package me.egg82.antivpn.api.event.api;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.AbstractEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

public class GenericAPILoadedEvent extends AbstractEvent implements APILoadedEvent {
    public GenericAPILoadedEvent(@NonNull VPNAPI api) {
        super(api);
    }
}

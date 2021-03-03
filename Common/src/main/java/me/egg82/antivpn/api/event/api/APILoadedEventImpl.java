package me.egg82.antivpn.api.event.api;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.AbstractEvent;
import org.jetbrains.annotations.NotNull;

public class GenericAPILoadedEvent extends AbstractEvent implements APILoadedEvent {
    public GenericAPILoadedEvent(@NotNull VPNAPI api) {
        super(api);
    }
}

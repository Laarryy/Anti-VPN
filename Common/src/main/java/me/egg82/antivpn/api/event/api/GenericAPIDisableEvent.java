package me.egg82.antivpn.api.event.api;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.AbstractEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

public class GenericAPIDisableEvent extends AbstractEvent implements APIDisableEvent {
    public GenericAPIDisableEvent(@NonNull VPNAPI api) {
        super(api);
    }
}

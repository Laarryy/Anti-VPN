package me.egg82.antivpn.api.event.api;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.AbstractEvent;
import org.jetbrains.annotations.NotNull;

public class GenericAPIDisableEvent extends AbstractEvent implements APIDisableEvent {
    public GenericAPIDisableEvent(@NotNull VPNAPI api) {
        super(api);
    }
}

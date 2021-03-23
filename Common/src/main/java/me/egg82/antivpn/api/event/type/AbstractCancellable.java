package me.egg82.antivpn.api.event.type;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.AbstractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractCancellable extends AbstractEvent implements Cancellable {
    protected final @NotNull AtomicBoolean cancelState = new AtomicBoolean(false);

    protected AbstractCancellable(@NotNull VPNAPI api) {
        super(api);
    }

    @Override
    @NotNull
    public AtomicBoolean cancellationState() { return cancelState; }
}

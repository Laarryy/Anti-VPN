package me.egg82.antivpn.api.event.type;

import java.util.concurrent.atomic.AtomicBoolean;
import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.AbstractEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractCancellable extends AbstractEvent implements Cancellable {
    protected AtomicBoolean cancelState = new AtomicBoolean(false);

    protected AbstractCancellable(@NonNull VPNAPI api) {
        super(api);
    }

    public @NonNull AtomicBoolean cancellationState() { return cancelState; }
}

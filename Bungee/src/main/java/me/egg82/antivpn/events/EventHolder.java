package me.egg82.antivpn.events;

import java.util.ArrayList;
import java.util.List;
import me.egg82.antivpn.api.VPNAPIProvider;
import ninja.egg82.events.BungeeEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EventHolder {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final List<BungeeEventSubscriber<?>> events = new ArrayList<>();

    protected final VPNAPI api = VPNAPIProvider.getInstance();

    public final int numEvents() { return events.size(); }

    public final void cancel() {
        for (BungeeEventSubscriber<?> event : events) {
            event.cancel();
        }
    }
}

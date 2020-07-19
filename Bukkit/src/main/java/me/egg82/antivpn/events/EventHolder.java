package me.egg82.antivpn.events;

import java.util.ArrayList;
import java.util.List;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.hooks.VaultHook;
import ninja.egg82.events.BukkitEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class EventHolder {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final List<BukkitEventSubscriber<?>> events = new ArrayList<>();

    protected final VPNAPI api = VPNAPI.getInstance();

    public final VaultHook vaultHook = new VaultHook();

    public final int numEvents() { return events.size(); }

    public final void cancel() {
        for (BukkitEventSubscriber<?> event : events) {
            event.cancel();
        }
    }
}

package me.egg82.antivpn.events;

import ninja.egg82.events.BungeeEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class EventHolder {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final List<BungeeEventSubscriber<?>> events = new ArrayList<>();

    public final int numEvents() {
        return events.size();
    }

    public final void cancel() {
        for (BungeeEventSubscriber<?> event : events) {
            event.cancel();
        }
    }
}

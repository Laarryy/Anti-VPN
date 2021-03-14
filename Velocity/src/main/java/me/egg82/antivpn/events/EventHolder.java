package me.egg82.antivpn.events;

import com.velocitypowered.api.event.PostOrder;
import ninja.egg82.events.PriorityEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class EventHolder {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final List<PriorityEventSubscriber<PostOrder, ?>> events = new ArrayList<>();

    public final int numEvents() { return events.size(); }

    public final void cancel() {
        for (PriorityEventSubscriber<PostOrder, ?> event : events) {
            event.cancel();
        }
    }
}

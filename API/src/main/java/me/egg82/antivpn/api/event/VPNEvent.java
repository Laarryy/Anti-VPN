package me.egg82.antivpn.api.event;

import me.egg82.antivpn.api.VPNAPI;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A superinterface for all Anti-VPN events.
 */
public interface VPNEvent {
    /**
     * Get the API instance this event was dispatched from
     *
     * @return the api instance
     */
    @NonNull VPNAPI getApi();

    /**
     * Gets the type of the event.
     *
     * @return the type of the event
     */
    @NonNull Class<? extends VPNEvent> getEventType();
}

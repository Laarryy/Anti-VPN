package me.egg82.antivpn.api.event;

import me.egg82.antivpn.api.VPNAPI;
import org.jetbrains.annotations.NotNull;

/**
 * A superinterface for all Anti-VPN events.
 */
public interface VPNEvent {
    /**
     * Get the API instance this event was dispatched from
     *
     * @return the api instance
     */
    @NotNull
    VPNAPI getApi();

    /**
     * Gets the type of the event.
     *
     * @return the type of the event
     */
    @NotNull
    Class<? super VPNEvent> getEventType();
}

package me.egg82.antivpn.api.event.api;

import me.egg82.antivpn.api.event.VPNEvent;
import net.kyori.event.EventBus;

/**
 * Called when the API is about to be disabled, and
 * the current {@link EventBus} will stop sending events.
 *
 * <p>This should only be fired if the Anti-VPN plugin is externally disabled.
 * Anti-VPN plugin reloads from the plugin's command will not trigger this,
 * as the event bus will not be destroyed.</p>
 */
public interface APIDisableEvent extends VPNEvent { }

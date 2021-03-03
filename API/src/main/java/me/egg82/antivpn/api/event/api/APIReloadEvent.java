package me.egg82.antivpn.api.event.api;

import me.egg82.antivpn.api.event.VPNEvent;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.api.model.source.SourceManager;
import org.jetbrains.annotations.NotNull;

/**
 * Called when the API is about to be reloaded.
 */
public interface APIReloadEvent extends VPNEvent {
    /**
     * Gets the new {@link IPManager} instance
     *
     * @return the new IP manager instance
     */
    @NotNull IPManager getNewIPManager();

    /**
     * Gets the new {@link PlayerManager} instance
     *
     * @return the new player manager instance
     */
    @NotNull PlayerManager getNewPlayerManager();

    /**
     * Gets the new {@link SourceManager} instance
     *
     * @return the new source manager instance
     */
    @NotNull SourceManager getNewSourceManager();
}

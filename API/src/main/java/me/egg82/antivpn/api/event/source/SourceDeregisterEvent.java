package me.egg82.antivpn.api.event.source;

import me.egg82.antivpn.api.event.VPNEvent;
import me.egg82.antivpn.api.event.type.Cancellable;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Called when a source is about to be removed from the sources list
 */
public interface SourceDeregisterEvent extends VPNEvent, Cancellable {
    /**
     * Gets the {@link Source} to be removed
     *
     * @return the source to be removed
     */
    @NonNull Source<? extends SourceModel> getSource();
}

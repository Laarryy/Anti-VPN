package me.egg82.antivpn.api.event.type;

import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents an event that can be cancelled
 */
public interface Cancellable {
    /**
     * Gets an {@link AtomicBoolean} holding the cancellation state of the event
     *
     * @return the cancellation
     */
    @NonNull AtomicBoolean cancellationState();

    /**
     * Returns true if the event is currently cancelled.
     *
     * @return if the event is cancelled
     */
    default boolean isCancelled() { return cancellationState().get(); }

    /**
     * Returns true if the event is not currently cancelled.
     *
     * @return if the event is not cancelled
     */
    default boolean isNotCancelled() { return !cancellationState().get(); }

    /**
     * Sets the cancellation state of the event.
     *
     * @param cancelled the new state
     * @return the previous state
     */
    default boolean setCancelled(boolean cancelled) { return cancellationState().getAndSet(cancelled); }
}

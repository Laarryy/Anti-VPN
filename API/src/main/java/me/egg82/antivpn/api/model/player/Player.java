package me.egg82.antivpn.api.model.player;

import java.io.Serializable;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A player which contains information about various statuses they hold.
 */
public interface Player extends Serializable {
    /**
     * Gets the player's {@link UUID}.
     *
     * @return the player's UUID
     */
    @NonNull UUID getUuid();

    /**
     * Gets the player's username.
     *
     * <p>Returns null if no username is known for the user.</p>
     *
     * @return the player's username
     */
    @Nullable String getName();

    /**
     * Returns the player's MCLeaks status.
     *
     * @return true if the player is an MCLeaks account, false if not
     */
    boolean isMcLeaks();

    /**
     * Sets the player's MCLeaks status.
     *
     * @param status true if the player is an MCLeaks account, false if not
     */
    void setMcLeaks(boolean status);

    /**
     * {@inheritDoc}
     */
    boolean equals(Object o);

    /**
     * {@inheritDoc}
     */
    int hashCode();

    /**
     * {@inheritDoc}
     */
    String toString();
}

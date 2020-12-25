package me.egg82.antivpn.api.platform;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Provides information about the platform AntiVPN is running on.
 */
public interface Platform {
    /**
     * Gets the type of platform AntiVPN is running on.
     *
     * @return the type of platform AntiVPN is running on
     */
    Platform.@NonNull Type getType();

    /**
     * Gets the unique players which have connected to the server since it started.
     *
     * @return the unique players
     */
    @NonNull Set<UUID> getUniquePlayers();

    /**
     * Gets the unique IPs which have connected to the server since it started.
     *
     * @return the unique IPs
     */
    @NonNull Set<String> getUniqueIps();

    /**
     * Gets the time when the plugin first started.
     *
     * @return the enable time
     */
    @NonNull Instant getStartTime();

    /**
     * Represents a type of platform which AntiVPN can run on.
     */
    enum Type {
        BUKKIT("Bukkit"),
        BUNGEECORD("BungeeCord"),
        VELOCITY("Velocity");

        private final String friendlyName;

        Type(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        /**
         * Gets a readable name for the platform type.
         *
         * @return a readable name
         */
        public @NonNull String getFriendlyName() { return this.friendlyName; }
    }
}

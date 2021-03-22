package me.egg82.antivpn.api.platform;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Provides information about the platform Anti-VPN is running on.
 */
public interface Platform {
    /**
     * Gets the type of platform Anti-VPN is running on.
     *
     * @return the type of platform Anti-VPN is running on
     */
    @NotNull
    Platform.Type getType();

    /**
     * Gets the unique players which have connected to the server since it started.
     *
     * @return the unique players
     */
    @NotNull
    Set<@NotNull UUID> getUniquePlayers();

    /**
     * Gets the unique IPs which have connected to the server since it started.
     *
     * @return the unique IPs
     */
    @NotNull
    Set<@NotNull InetAddress> getUniqueIPs();

    /**
     * Gets the time when the plugin first started.
     *
     * @return the enable time
     */
    @NotNull
    Instant getStartTime();

    /**
     * Represents a type of platform which Anti-VPN can run on.
     */
    enum Type {
        PAPER("Paper"),
        SPIGOT("Spigot"),
        BUNGEECORD("BungeeCord"),
        VELOCITY("Velocity");

        private final String friendlyName;

        Type(@NotNull String friendlyName) {
            this.friendlyName = friendlyName;
        }

        /**
         * Gets a readable name for the platform type.
         *
         * @return a readable name
         */
        public @NotNull String getFriendlyName() { return this.friendlyName; }
    }
}

package me.egg82.antivpn.api.model.source;

import me.egg82.antivpn.api.model.ip.IP;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An API source which contains information about various statuses it holds and methods for interfacing with the API.
 */
public interface Source {
    /**
     * Gets the source's name.
     *
     * @return the source's name
     */
    @NonNull String getName();

    /**
     * Returns the source's reliance on a valid key to function.
     *
     * @return true if the source requires a valid key to function, false if not
     */
    boolean isKeyRequired();

    /**
     * Returns the result from the source given its configuration.
     *
     * @param ip the {@link IP} object to test
     * @return true if the source thinks the IP is a VPN/proxy, false if not
     * @throws NullPointerException if the IP is null
     */
    default boolean getResult(@NonNull IP ip) { return getResult(ip.getIp()); }

    /**
     * Returns the result from the source given its configuration.
     *
     * @param ip the IP to test
     * @return true if the source thinks the IP is a VPN/proxy, false if not
     * @throws NullPointerException if the IP is null
     */
    boolean getResult(@NonNull String ip);
}

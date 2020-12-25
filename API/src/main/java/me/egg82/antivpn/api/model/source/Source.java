package me.egg82.antivpn.api.model.source;

import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.ip.IP;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An API source which contains information about various statuses it holds and methods for interfacing with the API.
 */
public interface Source<T extends SourceModel> {
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
     * @throws APIException if the source returned an error
     */
    default boolean getResult(@NonNull IP ip) throws APIException { return getResult(ip.getIp()); }

    /**
     * Returns the result from the source given its configuration.
     *
     * @param ip the IP to test
     * @return true if the source thinks the IP is a VPN/proxy, false if not
     * @throws NullPointerException if the IP is null
     * @throws APIException if the source returned an error
     * @throws IllegalArgumentException if the IP provided is invalid
     */
    boolean getResult(@NonNull String ip) throws APIException;

    /**
     * Returns the raw response from the source given its configuration.
     *
     * @param ip the IP to test
     * @return the raw response model from the source
     * @throws NullPointerException if the IP is null
     * @throws APIException if the source returned an error
     * @throws IllegalArgumentException if the IP provided is invalid
     */
    T getRawResponse(@NonNull String ip) throws APIException;
}

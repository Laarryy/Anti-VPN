package me.egg82.antivpn.api.model.source;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.ip.IP;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import org.jetbrains.annotations.NotNull;

/**
 * An API source which contains information about various statuses it holds and methods for interfacing with the API.
 */
public interface Source<T extends SourceModel> {
    /**
     * Gets the source's name.
     *
     * @return the source's name
     */
    @NotNull String getName();

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
     * @return a {@link CompletableFuture} - true if the source thinks the IP is a VPN/proxy, false if not
     * @throws NullPointerException if the IP is null
     * @throws APIException in the result if the source returned an error
     */
    default @NotNull CompletableFuture<@NotNull Boolean> getResult(@NotNull IP ip) { return getResult(ip.getIP().getHostAddress()); }

    /**
     * Returns the result from the source given its configuration.
     *
     * @param ip the IP to test
     * @return a {@link CompletableFuture} - true if the source thinks the IP is a VPN/proxy, false if not
     * @throws NullPointerException if the IP is null
     * @throws IllegalArgumentException if the IP provided is invalid
     * @throws APIException in the result if the source returned an error
     */
    default @NotNull CompletableFuture<@NotNull Boolean> getResult(@NotNull InetAddress ip) { return getResult(ip.getHostAddress()); }

    /**
     * Returns the result from the source given its configuration.
     *
     * @param ip the IP to test
     * @return a {@link CompletableFuture} - true if the source thinks the IP is a VPN/proxy, false if not
     * @throws NullPointerException if the IP is null
     * @throws IllegalArgumentException if the IP provided is invalid
     * @throws APIException in the result if the source returned an error
     */
    @NotNull CompletableFuture<@NotNull Boolean> getResult(@NotNull String ip);

    /**
     * Returns the raw response from the source given its configuration.
     *
     * @param ip the IP to test
     * @return a {@link CompletableFuture} - the raw response model from the source
     * @throws NullPointerException if the IP is null
     * @throws IllegalArgumentException if the IP provided is invalid
     * @throws APIException in the result if the source returned an error
     */
    default @NotNull CompletableFuture<@NotNull T> getRawResponse(@NotNull IP ip) { return getRawResponse(ip.getIP().getHostAddress()); }

    /**
     * Returns the raw response from the source given its configuration.
     *
     * @param ip the IP to test
     * @return a {@link CompletableFuture} - the raw response model from the source
     * @throws NullPointerException if the IP is null
     * @throws IllegalArgumentException if the IP provided is invalid
     * @throws APIException in the result if the source returned an error
     */
    default @NotNull CompletableFuture<@NotNull T> getRawResponse(@NotNull InetAddress ip) { return getRawResponse(ip.getHostAddress()); }

    /**
     * Returns the raw response from the source given its configuration.
     *
     * @param ip the IP to test
     * @return a {@link CompletableFuture} - the raw response model from the source
     * @throws NullPointerException if the IP is null
     * @throws IllegalArgumentException if the IP provided is invalid
     * @throws APIException in the result if the source returned an error
     */
    @NotNull CompletableFuture<@NotNull T> getRawResponse(@NotNull String ip);

    /**
     * Returns the class of the {@link SourceModel} used for this source.
     *
     * @return the class of the SourceModel for this source.
     */
    @NotNull Class<T> getModelClass();
}

package me.egg82.antivpn.api.model.ip;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import me.egg82.antivpn.api.APIException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the object responsible for managing {@link IP} instances.
 *
 * <p>Note that IP instances are automatically loaded for online players.
 * It's likely that offline players will not have an instance pre-loaded.</p>
 *
 * <p>All blocking methods return {@link CompletableFuture}s, which will be
 * populated with the result once the data has been loaded/saved asynchronously.
 * Care should be taken when using such methods to ensure that the main server
 * thread is not blocked.</p>
 *
 * <p>Methods such as {@link CompletableFuture#get()} and equivalent should
 * <strong>not</strong> be called on the main server thread. If you need to use
 * the result of these operations on the main server thread, register a
 * callback using {@link CompletableFuture#thenAcceptAsync(Consumer, Executor)}.</p>
 */
public interface IPManager {
    /**
     * Gets an IP object.
     *
     * @param ip the IP of the IP object to get
     * @return an {@link IP} object, if one matching the IP is available, or null if not
     * @throws NullPointerException if ip is null
     * @throws IllegalArgumentException if the IP provided is invalid
     */
    @Nullable IP getIp(@NonNull String ip);

    /**
     * Saves an IP back to the plugin's storage provider.
     *
     * <p>You should call this after you make any changes to an IP.</p>
     *
     * @param ip the IP object to save
     * @return a future to encapsulate the operation.
     * @throws NullPointerException if ip is null
     */
    @NonNull CompletableFuture<Void> saveIp(@NonNull IP ip);

    /**
     * Deletes any data about a given IP from the system.
     *
     * @param ip the {@link IP} object to delete
     * @return a future encapsulating the result of the operation
     */
    default @NonNull CompletableFuture<Void> deleteIp(@NonNull IP ip) { return deleteIp(ip.getIp()); }

    /**
     * Deletes any data about a given IP from the system.
     *
     * @param ip the IP
     * @return a future encapsulating the result of the operation
     * @throws IllegalArgumentException if the IP provided is invalid
     */
    @NonNull CompletableFuture<Void> deleteIp(@NonNull String ip);

    /**
     * Gets a set of all known IPs.
     *
     * @return a set of IPs
     */
    @NonNull CompletableFuture<Set<String>> getIps();

    /**
     * Gets the {@link AlgorithmMethod}, which represents the algorithm method
     * AntiVPN is currently using to determine IP statuses.
     *
     * @return the algorithm method
     */
    @NonNull AlgorithmMethod getCurrentAlgorithmMethod();

    /**
     * Gets the cascade result from AntiVPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use AntiVPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the sources.</p>
     *
     * @param ip The IP to test
     * @param useCache true if you would like to use AntiVPN's internal cache, false if not
     * @return true if the cascade algorithm thinks the provided IP is a VPN/proxy, false if not
     * @throws APIException if a result could not be obtained
     */
    default boolean cascade(@NonNull IP ip, boolean useCache) throws APIException { return cascade(ip.getIp(), useCache); }

    /**
     * Gets the cascade result from AntiVPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use AntiVPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the sources.</p>
     *
     * @param ip The IP to test
     * @param useCache true if you would like to use AntiVPN's internal cache, false if not
     * @return true if the cascade algorithm thinks the provided IP is a VPN/proxy, false if not
     * @throws APIException if a result could not be obtained
     */
    boolean cascade(@NonNull String ip, boolean useCache) throws APIException;

    /**
     * Gets the consensus result from AntiVPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use AntiVPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the sources.</p>
     *
     * @param ip The IP to test
     * @param useCache true if you would like to use AntiVPN's internal cache, false if not
     * @return a number between 0 and 1 determining the likelihood that an IP is a VPN/proxy
     * @throws APIException if a result could not be obtained
     */
    default double consensus(@NonNull IP ip, boolean useCache) throws APIException { return consensus(ip.getIp(), useCache); }

    /**
     * Gets the consensus result from AntiVPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use AntiVPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the sources.</p>
     *
     * @param ip The IP to test
     * @param useCache true if you would like to use AntiVPN's internal cache, false if not
     * @return a number between 0 and 1 determining the likelihood that an IP is a VPN/proxy
     * @throws APIException if a result could not be obtained
     */
    double consensus(@NonNull String ip, boolean useCache) throws APIException;
}

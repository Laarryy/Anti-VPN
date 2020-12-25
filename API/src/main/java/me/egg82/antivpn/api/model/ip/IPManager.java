package me.egg82.antivpn.api.model.ip;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
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
     */
    @NonNull CompletableFuture<Void> deleteIp(@NonNull String ip);

    /**
     * Gets a set of all known IPs.
     *
     * @return a set of IPs
     */
    @NonNull CompletableFuture<Set<String>> getIps();

    /**
     * Gets the {@link AlgorithmMethod}, which represents the algorithm method the
     * plugin is using to determine IP statuses.
     *
     * @return the algorithm method
     */
    @NonNull AlgorithmMethod getCurrentAlgorithmMethod();
}

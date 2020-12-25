package me.egg82.antivpn.api.model.player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import me.egg82.antivpn.api.APIException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the object responsible for managing {@link Player} instances.
 *
 * <p>Note that Player instances are automatically loaded for online players.
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
public interface PlayerManager {
    /**
     * Gets a player.
     *
     * @param uniqueId the {@link UUID} of the player to get
     * @return a {@link CompletableFuture} - a {@link Player} object, if one matching the uuid is available, or null if not
     * @throws NullPointerException if the uuid is null
     */
    @Nullable CompletableFuture<Player> getPlayer(@NonNull UUID uniqueId);

    /**
     * Gets a player.
     *
     * @param username the username of the player to get
     * @return a {@link CompletableFuture} - a {@link Player} object, if one matching the name is available, or null if not
     * @throws NullPointerException if the name is null
     */
    @Nullable CompletableFuture<Player> getPlayer(@NonNull String username);

    /**
     * Saves a player back to the plugin's storage provider.
     *
     * <p>You should call this after you make any changes to a player.</p>
     *
     * @param player the player to save
     * @return a future to encapsulate the operation.
     * @throws NullPointerException if player is null
     */
    @NonNull CompletableFuture<Void> savePlayer(@NonNull Player player);

    /**
     * Deletes any data about a given player from the system.
     *
     * @param player the {@link Player} object to delete
     * @return a future encapsulating the result of the operation
     * @throws NullPointerException if player is null
     */
    default @NonNull CompletableFuture<Void> deletePlayer(@NonNull Player player) { return deletePlayer(player.getUuid()); }

    /**
     * Deletes any data about a given player from the system.
     *
     * @param uniqueId the player's {@link UUID}
     * @return a future encapsulating the result of the operation
     * @throws NullPointerException if the uuid is null
     */
    @NonNull CompletableFuture<Void> deletePlayer(@NonNull UUID uniqueId);

    /**
     * Gets a set of all known player {@link UUID}s.
     *
     * @return a set of UUIDs
     */
    @NonNull CompletableFuture<Set<UUID>> getPlayers();

    /**
     * Gets the MCLeaks result from AntiVPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use AntiVPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the API.</p>
     *
     * @param player The player to test
     * @param useCache true if you would like to use AntiVPN's internal cache, false if not
     * @return a {@link CompletableFuture} - true if the API thinks the provided player is an MCLeaks account, false if not
     * @throws NullPointerException if player is null
     * @throws APIException in the result if a result could not be obtained
     */
    default @NonNull CompletableFuture<Boolean> checkMcLeaks(@NonNull Player player, boolean useCache) { return checkMcLeaks(player.getUuid(), useCache); }

    /**
     * Gets the MCLeaks result from AntiVPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use AntiVPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the API.</p>
     *
     * @param uniqueId The player {@link UUID} to test
     * @param useCache true if you would like to use AntiVPN's internal cache, false if not
     * @return a {@link CompletableFuture} - true if the API thinks the provided player is an MCLeaks account, false if not
     * @throws NullPointerException if the uuid is null
     * @throws APIException in the result if a result could not be obtained
     */
    @NonNull CompletableFuture<Boolean> checkMcLeaks(@NonNull UUID uniqueId, boolean useCache);
}

package me.egg82.antivpn.api.model.player;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.ip.IP;
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
    @NonNull CompletableFuture<Player> getPlayer(@NonNull UUID uniqueId);

    /**
     * Gets a player.
     *
     * @param username the username of the player to get
     * @return a {@link CompletableFuture} - a {@link Player} object, if one matching the name is available, or null if not
     * @throws NullPointerException if the name is null
     */
    @NonNull CompletableFuture<Player> getPlayer(@NonNull String username);

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
     * Gets the MCLeaks result from Anti-VPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use Anti-VPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the API.</p>
     *
     * @param player The player to test
     * @param useCache true if you would like to use Anti-VPN's internal cache, false if not
     * @return a {@link CompletableFuture} - true if the API thinks the provided player is an MCLeaks account, false if not
     * @throws NullPointerException if player is null
     * @throws APIException in the result if a result could not be obtained
     */
    default @NonNull CompletableFuture<Boolean> checkMcLeaks(@NonNull Player player, boolean useCache) { return checkMcLeaks(player.getUuid(), useCache); }

    /**
     * Gets the MCLeaks result from Anti-VPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use Anti-VPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the API.</p>
     *
     * @param uniqueId The player {@link UUID} to test
     * @param useCache true if you would like to use Anti-VPN's internal cache, false if not
     * @return a {@link CompletableFuture} - true if the API thinks the provided player is an MCLeaks account, false if not
     * @throws NullPointerException if the uuid is null
     * @throws APIException in the result if a result could not be obtained
     */
    @NonNull CompletableFuture<Boolean> checkMcLeaks(@NonNull UUID uniqueId, boolean useCache);

    /**
     * Runs the player through the kick/command procedure
     * for MCLeaks usage, as outlined by Anti-VPN's config.
     *
     * @param player The {@link Player} to kick
     * @param ip The {@link IP} of the player to kick
     * @return true if the player was online, false if not
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    default boolean kickForMcLeaks(@NonNull Player player, @NonNull IP ip) { return kickForMcLeaks(Objects.requireNonNull(player.getName()), player.getUuid(), ip.getIP().getHostAddress()); }

    /**
     * Runs the player through the kick/command procedure
     * for MCLeaks usage, as outlined by Anti-VPN's config.
     *
     * @param playerName The player name to kick
     * @param playerUuid The player UUID to kick
     * @param ip The IP of the player to kick
     * @return true if the player was online, false if not
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    default boolean kickForMcLeaks(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull IP ip) { return kickForMcLeaks(playerName, playerUuid, ip.getIP().getHostAddress()); }

    /**
     * Runs the player through the kick/command procedure
     * for MCLeaks usage, as outlined by Anti-VPN's config.
     *
     * @param player The {@link Player} to kick
     * @param ip The IP of the player to kick
     * @return true if the player was online, false if not
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    default boolean kickForMcLeaks(@NonNull Player player, @NonNull InetAddress ip) { return kickForMcLeaks(Objects.requireNonNull(player.getName()), player.getUuid(), ip.getHostAddress()); }

    /**
     * Runs the player through the kick/command procedure
     * for MCLeaks usage, as outlined by Anti-VPN's config.
     *
     * @param player The {@link Player} to kick
     * @param ip The IP of the player to kick
     * @return true if the player was online, false if not
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    default boolean kickForMcLeaks(@NonNull Player player, @NonNull String ip) { return kickForMcLeaks(Objects.requireNonNull(player.getName()), player.getUuid(), ip); }

    /**
     * Runs the player through the kick/command procedure
     * for MCLeaks usage, as outlined by Anti-VPN's config.
     *
     * @param playerName The player name to kick
     * @param playerUuid The player UUID to kick
     * @param ip The IP of the player to kick
     * @return true if the player was online, false if not
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    default boolean kickForMcLeaks(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull InetAddress ip) { return kickForMcLeaks(playerName, playerUuid, ip.getHostAddress()); };

    /**
     * Runs the player through the kick/command procedure
     * for MCLeaks usage, as outlined by Anti-VPN's config.
     *
     * @param playerName The player name to kick
     * @param playerUuid The player UUID to kick
     * @param ip The IP of the player to kick
     * @return true if the player was online, false if not
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    boolean kickForMcLeaks(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull String ip);

    /**
     * Returns the MCLeaks kick message as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param player The {@link Player} to tailor the message to
     * @param ip The {@link IP} of the player to tailor the message to
     * @return the MCLeaks kick message, as defined in Anti-VPN's config
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    default @Nullable String getMcLeaksKickMessage(@NonNull Player player, @NonNull IP ip) { return getMcLeaksKickMessage(Objects.requireNonNull(player.getName()), player.getUuid(), ip.getIP().getHostAddress()); }

    /**
     * Returns the MCLeaks kick message as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param playerName The player name to tailor the message to
     * @param playerUuid The player UUID to tailor the message to
     * @param ip The IP of the player to tailor the message to
     * @return the MCLeaks kick message, as defined in Anti-VPN's config
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    default @Nullable String getMcLeaksKickMessage(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull IP ip) { return getMcLeaksKickMessage(playerName, playerUuid, ip.getIP().getHostAddress()); }

    /**
     * Returns the MCLeaks kick message as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param player The {@link Player} to tailor the message to
     * @param ip The IP of the player to tailor the message to
     * @return the MCLeaks kick message, as defined in Anti-VPN's config
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    default @Nullable String getMcLeaksKickMessage(@NonNull Player player, @NonNull InetAddress ip) { return getMcLeaksKickMessage(Objects.requireNonNull(player.getName()), player.getUuid(), ip.getHostAddress()); }

    /**
     * Returns the MCLeaks kick message as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param player The {@link Player} to tailor the message to
     * @param ip The IP of the player to tailor the message to
     * @return the MCLeaks kick message, as defined in Anti-VPN's config
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    default @Nullable String getMcLeaksKickMessage(@NonNull Player player, @NonNull String ip) { return getMcLeaksKickMessage(Objects.requireNonNull(player.getName()), player.getUuid(), ip); }

    /**
     * Returns the MCLeaks kick message as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param playerName The player name to tailor the message to
     * @param playerUuid The player UUID to tailor the message to
     * @param ip The IP of the player to tailor the message to
     * @return the MCLeaks kick message, as defined in Anti-VPN's config
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    default @Nullable String getMcLeaksKickMessage(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull InetAddress ip) { return getMcLeaksKickMessage(playerName, playerUuid, ip.getHostAddress()); }

    /**
     * Returns the MCLeaks kick message as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param playerName The player name to tailor the message to
     * @param playerUuid The player UUID to tailor the message to
     * @param ip The IP of the player to tailor the message to
     * @return the MCLeaks kick message, as defined in Anti-VPN's config
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    @Nullable String getMcLeaksKickMessage(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull String ip);

    /**
     * Returns the MCLeaks commands as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param player The {@link Player} to tailor the commands to
     * @param ip The {@link IP} of the player to tailor the commands to
     * @return a {@link List} of MCLeaks commands to run, as defined in Anti-VPN's config
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    default @NonNull List<String> getMcLeaksCommands(@NonNull Player player, @NonNull IP ip) { return getMcLeaksCommands(Objects.requireNonNull(player.getName()), player.getUuid(), ip.getIP().getHostAddress()); }

    /**
     * Returns the MCLeaks commands as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param playerName The player name to tailor the commands to
     * @param playerUuid The player UUID to tailor the commands to
     * @param ip The IP of the player to tailor the commands to
     * @return a {@link List} of MCLeaks commands to run, as defined in Anti-VPN's config
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    default @NonNull List<String> getMcLeaksCommands(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull IP ip) { return getMcLeaksCommands(playerName, playerUuid, ip.getIP().getHostAddress()); }

    /**
     * Returns the MCLeaks commands as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param player The {@link Player} to tailor the commands to
     * @param ip The IP of the player to tailor the commands to
     * @return a {@link List} of MCLeaks commands to run, as defined in Anti-VPN's config
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    default @NonNull List<String> getMcLeaksCommands(@NonNull Player player, @NonNull InetAddress ip) { return getMcLeaksCommands(Objects.requireNonNull(player.getName()), player.getUuid(), ip.getHostAddress()); }

    /**
     * Returns the MCLeaks commands as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param player The {@link Player} to tailor the commands to
     * @param ip The IP of the player to tailor the commands to
     * @return a {@link List} of MCLeaks commands to run, as defined in Anti-VPN's config
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    default @NonNull List<String> getMcLeaksCommands(@NonNull Player player, @NonNull String ip) { return getMcLeaksCommands(Objects.requireNonNull(player.getName()), player.getUuid(), ip); }

    /**
     * Returns the MCLeaks commands as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param playerName The player name to tailor the commands to
     * @param playerUuid The player UUID to tailor the commands to
     * @param ip The IP of the player to tailor the commands to
     * @return a {@link List} of MCLeaks commands to run, as defined in Anti-VPN's config
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    default @NonNull List<String> getMcLeaksCommands(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull InetAddress ip) { return getMcLeaksCommands(playerName, playerUuid, ip.getHostAddress()); }

    /**
     * Returns the MCLeaks commands as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param playerName The player name to tailor the commands to
     * @param playerUuid The player UUID to tailor the commands to
     * @param ip The IP of the player to tailor the commands to
     * @return a {@link List} of MCLeaks commands to run, as defined in Anti-VPN's config
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    @NonNull List<String> getMcLeaksCommands(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull String ip);
}

package me.egg82.antivpn.api.model.ip;

import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.player.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

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
     *
     * @return a {@link CompletableFuture} - an {@link IP} object, if one matching the IP is available, or null if not
     *
     * @throws NullPointerException if ip is null
     * @throws IllegalArgumentException if the IP provided is invalid
     */
    @NotNull
    default CompletableFuture<@Nullable IP> getIP(@NotNull InetAddress ip) { return getIP(ip.getHostAddress()); }

    /**
     * Gets an IP object.
     *
     * @param ip the IP of the IP object to get
     *
     * @return a {@link CompletableFuture} - an {@link IP} object, if one matching the IP is available, or null if not
     *
     * @throws NullPointerException if ip is null
     * @throws IllegalArgumentException if the IP provided is invalid
     */
    @NotNull
    CompletableFuture<@Nullable IP> getIP(@NotNull String ip);

    /**
     * Saves an IP back to the plugin's storage provider.
     *
     * <p>You should call this after you make any changes to an IP.</p>
     *
     * @param ip the IP object to save
     *
     * @return a {@link CompletableFuture} to encapsulate the operation.
     *
     * @throws NullPointerException if ip is null
     */
    @NotNull
    CompletableFuture<Void> saveIP(@NotNull IP ip);

    /**
     * Deletes any data about a given IP from the system.
     *
     * @param ip the {@link IP} object to delete
     *
     * @return a {@link CompletableFuture} encapsulating the result of the operation
     *
     * @throws NullPointerException if ip is null
     */
    @NotNull
    default CompletableFuture<Void> deleteIP(@NotNull IP ip) { return deleteIP(ip.getIP().getHostAddress()); }

    /**
     * Deletes any data about a given IP from the system.
     *
     * @param ip the IP
     *
     * @return a {@link CompletableFuture} encapsulating the result of the operation
     *
     * @throws IllegalArgumentException if the IP provided is invalid
     * @throws NullPointerException if ip is null
     */
    @NotNull
    default CompletableFuture<Void> deleteIP(@NotNull InetAddress ip) { return deleteIP(ip.getHostAddress()); }

    /**
     * Deletes any data about a given IP from the system.
     *
     * @param ip the IP
     *
     * @return a {@link CompletableFuture} encapsulating the result of the operation
     *
     * @throws IllegalArgumentException if the IP provided is invalid
     * @throws NullPointerException if ip is null
     */
    @NotNull
    CompletableFuture<Void> deleteIP(@NotNull String ip);

    /**
     * Gets a set of all known IPs.
     *
     * @return a {@link CompletableFuture} - a set of IPs
     */
    @NotNull
    CompletableFuture<@NotNull Set<@NotNull InetAddress>> getIPs();

    /**
     * Gets the {@link AlgorithmMethod}, which represents the algorithm method
     * Anti-VPN is currently using to determine IP statuses.
     *
     * @return the algorithm method
     *
     * @throws IllegalStateException if the configuration could not be retrieved
     */
    @NotNull
    AlgorithmMethod getCurrentAlgorithmMethod();

    /**
     * Gets the cascade result from Anti-VPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use Anti-VPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the sources.</p>
     *
     * @param ip The IP to test
     * @param useCache true if you would like to use Anti-VPN's internal cache, false if not
     *
     * @return a {@link CompletableFuture} - true if the cascade algorithm thinks the provided IP is a VPN/proxy, false if not
     *
     * @throws NullPointerException if ip is null
     * @throws APIException in the result if a result could not be obtained
     */
    @NotNull
    default CompletableFuture<@NotNull Boolean> cascade(@NotNull IP ip, boolean useCache) { return cascade(ip.getIP().getHostAddress(), useCache); }

    /**
     * Gets the cascade result from Anti-VPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use Anti-VPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the sources.</p>
     *
     * @param ip The IP to test
     * @param useCache true if you would like to use Anti-VPN's internal cache, false if not
     *
     * @return a {@link CompletableFuture} - true if the cascade algorithm thinks the provided IP is a VPN/proxy, false if not
     *
     * @throws NullPointerException if ip is null
     * @throws APIException in the result if a result could not be obtained
     */
    @NotNull
    default CompletableFuture<@NotNull Boolean> cascade(@NotNull InetAddress ip, boolean useCache) { return cascade(ip.getHostAddress(), useCache); }

    /**
     * Gets the cascade result from Anti-VPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use Anti-VPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the sources.</p>
     *
     * @param ip The IP to test
     * @param useCache true if you would like to use Anti-VPN's internal cache, false if not
     *
     * @return a {@link CompletableFuture} - true if the cascade algorithm thinks the provided IP is a VPN/proxy, false if not
     *
     * @throws NullPointerException if ip is null
     * @throws APIException in the result if a result could not be obtained
     */
    @NotNull
    CompletableFuture<@NotNull Boolean> cascade(@NotNull String ip, boolean useCache);

    /**
     * Gets the consensus result from Anti-VPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use Anti-VPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the sources.</p>
     *
     * @param ip The IP to test
     * @param useCache true if you would like to use Anti-VPN's internal cache, false if not
     *
     * @return a {@link CompletableFuture} - a number between 0 and 1 determining the likelihood that an IP is a VPN/proxy
     *
     * @throws NullPointerException if ip is null
     * @throws APIException in the result if a result could not be obtained
     */
    @NotNull
    default CompletableFuture<@NotNull Double> consensus(@NotNull IP ip, boolean useCache) { return consensus(ip.getIP().getHostAddress(), useCache); }

    /**
     * Gets the consensus result from Anti-VPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use Anti-VPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the sources.</p>
     *
     * @param ip The IP to test
     * @param useCache true if you would like to use Anti-VPN's internal cache, false if not
     *
     * @return a {@link CompletableFuture} - a number between 0 and 1 determining the likelihood that an IP is a VPN/proxy
     *
     * @throws NullPointerException if ip is null
     * @throws APIException in the result if a result could not be obtained
     */
    @NotNull
    default CompletableFuture<@NotNull Double> consensus(@NotNull InetAddress ip, boolean useCache) { return consensus(ip.getHostAddress(), useCache); }

    /**
     * Gets the consensus result from Anti-VPN using the configuration
     * provided to the plugin.
     *
     * <p>You may choose to use Anti-VPN's internal cache
     * for the result, or skip the cache and get an answer
     * directly from the sources.</p>
     *
     * @param ip The IP to test
     * @param useCache true if you would like to use Anti-VPN's internal cache, false if not
     *
     * @return a {@link CompletableFuture} - a number between 0 and 1 determining the likelihood that an IP is a VPN/proxy
     *
     * @throws NullPointerException if ip is null
     * @throws APIException in the result if a result could not be obtained
     */
    @NotNull
    CompletableFuture<@NotNull Double> consensus(@NotNull String ip, boolean useCache);

    /**
     * Returns the minimum consensus value from
     * Anti-VPN's configuration.
     *
     * <p>This value is the minimum consensus needed
     * for an IP to be considered "bad" while running
     * in consensus mode.</p>
     *
     * @return the minimum consensus value
     *
     * @throws IllegalStateException if the configuration could not be retrieved
     */
    double getMinConsensusValue();

    /**
     * Runs the player through the kick/command procedure
     * for VPN usage, as outlined by Anti-VPN's config.
     *
     * @param player The {@link Player} to kick
     * @param ip The {@link IP} of the player to kick
     *
     * @return true if the player was online, false if not
     *
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    default boolean kickForVpn(@NotNull Player player, @NotNull IP ip) {
        return kickForVpn(
                Objects.requireNonNull(player.getName()),
                player.getUuid(),
                ip.getIP().getHostAddress()
        );
    }

    /**
     * Runs the player through the kick/command procedure
     * for VPN usage, as outlined by Anti-VPN's config.
     *
     * @param playerName The player name to kick
     * @param playerUuid The player UUID to kick
     * @param ip The IP of the player to kick
     *
     * @return true if the player was online, false if not
     *
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    default boolean kickForVpn(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull IP ip) {
        return kickForVpn(
                playerName,
                playerUuid,
                ip.getIP().getHostAddress()
        );
    }

    /**
     * Runs the player through the kick/command procedure
     * for VPN usage, as outlined by Anti-VPN's config.
     *
     * @param player The {@link Player} to kick
     * @param ip The IP of the player to kick
     *
     * @return true if the player was online, false if not
     *
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    default boolean kickForVpn(@NotNull Player player, @NotNull InetAddress ip) {
        return kickForVpn(
                Objects.requireNonNull(player.getName()),
                player.getUuid(),
                ip.getHostAddress()
        );
    }

    /**
     * Runs the player through the kick/command procedure
     * for VPN usage, as outlined by Anti-VPN's config.
     *
     * @param player The {@link Player} to kick
     * @param ip The IP of the player to kick
     *
     * @return true if the player was online, false if not
     *
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    default boolean kickForVpn(@NotNull Player player, @NotNull String ip) { return kickForVpn(Objects.requireNonNull(player.getName()), player.getUuid(), ip); }

    /**
     * Runs the player through the kick/command procedure
     * for VPN usage, as outlined by Anti-VPN's config.
     *
     * @param playerName The player name to kick
     * @param playerUuid The player UUID to kick
     * @param ip The IP of the player to kick
     *
     * @return true if the player was online, false if not
     *
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    default boolean kickForVpn(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull InetAddress ip) {
        return kickForVpn(
                playerName,
                playerUuid,
                ip.getHostAddress()
        );
    }

    /**
     * Runs the player through the kick/command procedure
     * for VPN usage, as outlined by Anti-VPN's config.
     *
     * @param playerName The player name to kick
     * @param playerUuid The player UUID to kick
     * @param ip The IP of the player to kick
     *
     * @return true if the player was online, false if not
     *
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    boolean kickForVpn(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip);

    /**
     * Returns the VPN kick message as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param player The {@link Player} to tailor the message to
     * @param ip The {@link IP} of the player to tailor the message to
     *
     * @return the VPN kick message, as defined in Anti-VPN's config
     *
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    @Nullable
    default Component getVpnKickMessage(@NotNull Player player, @NotNull IP ip) {
        return getVpnKickMessage(
                Objects.requireNonNull(player.getName()),
                player.getUuid(),
                ip.getIP().getHostAddress()
        );
    }

    /**
     * Returns the VPN kick message as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param playerName The player name to tailor the message to
     * @param playerUuid The player UUID to tailor the message to
     * @param ip The IP of the player to tailor the message to
     *
     * @return the VPN kick message, as defined in Anti-VPN's config
     *
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    @Nullable
    default Component getVpnKickMessage(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull IP ip) {
        return getVpnKickMessage(
                playerName,
                playerUuid,
                ip.getIP()
                        .getHostAddress()
        );
    }

    /**
     * Returns the VPN kick message as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param player The {@link Player} to tailor the message to
     * @param ip The IP of the player to tailor the message to
     *
     * @return the VPN kick message, as defined in Anti-VPN's config
     *
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    @Nullable
    default Component getVpnKickMessage(@NotNull Player player, @NotNull InetAddress ip) {
        return getVpnKickMessage(
                Objects.requireNonNull(player.getName()),
                player.getUuid(),
                ip.getHostAddress()
        );
    }

    /**
     * Returns the VPN kick message as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param player The {@link Player} to tailor the message to
     * @param ip The IP of the player to tailor the message to
     *
     * @return the VPN kick message, as defined in Anti-VPN's config
     *
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    @Nullable
    default Component getVpnKickMessage(@NotNull Player player, @NotNull String ip) {
        return getVpnKickMessage(
                Objects.requireNonNull(player.getName()),
                player.getUuid(),
                ip
        );
    }

    /**
     * Returns the VPN kick message as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param playerName The player name to tailor the message to
     * @param playerUuid The player UUID to tailor the message to
     * @param ip The IP of the player to tailor the message to
     *
     * @return the VPN kick message, as defined in Anti-VPN's config
     *
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    @Nullable
    default Component getVpnKickMessage(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull InetAddress ip) {
        return getVpnKickMessage(
                playerName,
                playerUuid,
                ip.getHostAddress()
        );
    }

    /**
     * Returns the VPN kick message as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param playerName The player name to tailor the message to
     * @param playerUuid The player UUID to tailor the message to
     * @param ip The IP of the player to tailor the message to
     *
     * @return the VPN kick message, as defined in Anti-VPN's config
     *
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    @Nullable
    Component getVpnKickMessage(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip);

    /**
     * Returns the VPN commands as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param player The {@link Player} to tailor the commands to
     * @param ip The {@link IP} of the player to tailor the commands to
     *
     * @return a {@link List} of VPN commands to run, as defined in Anti-VPN's config
     *
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    @Nullable
    default List<@NotNull String> getVpnCommands(@NotNull Player player, @NotNull IP ip) {
        return getVpnCommands(
                Objects.requireNonNull(player.getName()),
                player.getUuid(),
                ip.getIP().getHostAddress()
        );
    }

    /**
     * Returns the VPN commands as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param playerName The player name to tailor the commands to
     * @param playerUuid The player UUID to tailor the commands to
     * @param ip The IP of the player to tailor the commands to
     *
     * @return a {@link List} of VPN commands to run, as defined in Anti-VPN's config
     *
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    @Nullable
    default List<@NotNull String> getVpnCommands(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull IP ip) {
        return getVpnCommands(
                playerName,
                playerUuid,
                ip.getIP()
                        .getHostAddress()
        );
    }

    /**
     * Returns the VPN commands as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param player The {@link Player} to tailor the commands to
     * @param ip The IP of the player to tailor the commands to
     *
     * @return a {@link List} of VPN commands to run, as defined in Anti-VPN's config
     *
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    @NotNull
    default List<@NotNull String> getVpnCommands(
            @NotNull Player player,
            @NotNull InetAddress ip
    ) { return getVpnCommands(Objects.requireNonNull(player.getName()), player.getUuid(), ip.getHostAddress()); }

    /**
     * Returns the VPN commands as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param player The {@link Player} to tailor the commands to
     * @param ip The IP of the player to tailor the commands to
     *
     * @return a {@link List} of VPN commands to run, as defined in Anti-VPN's config
     *
     * @throws NullPointerException if player is null, player.getName() is null, or ip is null
     */
    @NotNull
    default List<@NotNull String> getVpnCommands(@NotNull Player player, @NotNull String ip) {
        return getVpnCommands(
                Objects.requireNonNull(player.getName()),
                player.getUuid(),
                ip
        );
    }

    /**
     * Returns the VPN commands as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param playerName The player name to tailor the commands to
     * @param playerUuid The player UUID to tailor the commands to
     * @param ip The IP of the player to tailor the commands to
     *
     * @return a {@link List} of VPN commands to run, as defined in Anti-VPN's config
     *
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    @NotNull
    default List<@NotNull String> getVpnCommands(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull InetAddress ip) {
        return getVpnCommands(
                playerName,
                playerUuid,
                ip.getHostAddress()
        );
    }

    /**
     * Returns the VPN commands as defined
     * in Anti-VPN's config, tailored to the player provided.
     *
     * @param playerName The player name to tailor the commands to
     * @param playerUuid The player UUID to tailor the commands to
     * @param ip The IP of the player to tailor the commands to
     *
     * @return a {@link List} of VPN commands to run, as defined in Anti-VPN's config
     *
     * @throws NullPointerException if playerName, playerUuid, or ip is null
     */
    @NotNull
    List<@NotNull String> getVpnCommands(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip);
}

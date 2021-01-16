package me.egg82.antivpn.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.event.VPNEvent;
import me.egg82.antivpn.api.model.ip.IP;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.Player;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.api.platform.Platform;
import me.egg82.antivpn.api.platform.PluginMetadata;
import net.engio.mbassy.bus.MBassador;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * The Anti-VPN API.
 *
 * <p>The API allows other plugins on the server to read and modify Anti-VPN
 * data, change behaviour of the plugin, listen to certain events, and integrate
 * Anti-VPN into other plugins and systems.</p>
 *
 * <p>This interface represents the base of the API package. All functions are
 * accessed via this interface.</p>
 *
 * <p>To start using the API, you need to obtain an instance of this interface.
 * These are registered by the Anti-VPN plugin.</p>
 *
 * <p>An instance can be obtained from the static singleton accessor in
 * {@link VPNAPIProvider}.</p>
 *
 * <p>A good portion of this API was taken from LuckPerms, if not at least inspired by it.
 * License available here: https://github.com/lucko/LuckPerms/blob/master/LICENSE.txt</p>
 */
public interface VPNAPI {
    /**
     * Gets the {@link UUID} of this server.
     *
     * <p>This is defined in the Anti-VPN stats-id file, and is used in
     * messaging services and server statistics (if enabled).</p>
     *
     * <p>The default server UUID is randomly-generated.</p>
     *
     * @return the server UUID
     */
    @NonNull UUID getServerId();

    /**
     * Gets the {@link IPManager}, responsible for managing
     * {@link IP} instances.
     *
     * <p>This manager can be used to retrieve instances of {@link IP} by name
     * or query all loaded IPs.</p>
     *
     * @return the IP manager
     */
    @NonNull IPManager getIPManager();

    /**
     * Gets the {@link PlayerManager}, responsible for managing
     * {@link Player} instances.
     *
     * <p>This manager can be used to retrieve instances of {@link Player} by uuid
     * or name, or query all loaded players.</p>
     *
     * @return the player manager
     */
    @NonNull PlayerManager getPlayerManager();

    /**
     * Gets the {@link SourceManager}, responsible for managing
     * {@link Source} instances.
     *
     * <p>This manager can be used to retrieve instances of {@link Source} by name
     * or query all loaded sources.</p>
     *
     * @return the source manager
     */
    @NonNull SourceManager getSourceManager();

    /**
     * Gets the {@link Platform}, which represents the server platform the
     * plugin is running on.
     *
     * @return the platform
     */
    @NonNull Platform getPlatform();

    /**
     * Gets the {@link PluginMetadata}, responsible for providing metadata about
     * the Anti-VPN plugin currently running.
     *
     * @return the plugin metadata
     */
    @NonNull PluginMetadata getPluginMetadata();

    /**
     * Schedules the execution of an update task, and returns an encapsulation
     * of the task as a {@link CompletableFuture}.
     *
     * <p>The exact actions performed in an update task remains an
     * implementation detail of the plugin, however, as a minimum, it is
     * expected to perform a full reload of player and IP data, and
     * ensure that any changes are fully applied and propagated.</p>
     *
     * @return a future
     */
    @NonNull CompletableFuture<Void> runUpdateTask();

    /**
     * Gets the {@link MBassador} event bus, used for subscribing to internal Anti-VPN
     * events.
     *
     * @return the event bus
     */
    @NonNull MBassador<VPNEvent> getEventBus();
}

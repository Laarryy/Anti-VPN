package me.egg82.antivpn.api.platform;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Provides information about the LuckPerms plugin.
 */
public interface PluginMetadata {
    /**
     * Gets the plugin version.
     *
     * @return the version of the plugin running on the platform
     */
    @NonNull String getVersion();

    /**
     * Gets the API version.
     *
     * @return the version of the API running on the platform
     */
    double getApiVersion();
}

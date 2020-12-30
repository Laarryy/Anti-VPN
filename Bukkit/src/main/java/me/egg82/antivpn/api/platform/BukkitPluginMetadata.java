package me.egg82.antivpn.api.platform;

import org.checkerframework.checker.nullness.qual.NonNull;

public class BukkitPluginMetadata extends AbstractPluginMetadata {
    private final String pluginVersion;

    public BukkitPluginMetadata(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public @NonNull String getVersion() { return pluginVersion; }
}

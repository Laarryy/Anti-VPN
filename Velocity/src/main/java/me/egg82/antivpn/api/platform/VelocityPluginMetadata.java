package me.egg82.antivpn.api.platform;

import org.checkerframework.checker.nullness.qual.NonNull;

public class VelocityPluginMetadata extends AbstractPluginMetadata {
    private final String pluginVersion;

    public VelocityPluginMetadata(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public @NonNull String getVersion() { return pluginVersion; }
}

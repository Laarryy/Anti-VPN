package me.egg82.antivpn.api.platform;

import org.jetbrains.annotations.NotNull;

public class VelocityPluginMetadata extends AbstractPluginMetadata {
    private final String pluginVersion;

    public VelocityPluginMetadata(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public @NotNull String getVersion() { return pluginVersion; }
}

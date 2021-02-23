package me.egg82.antivpn.api.platform;

import org.jetbrains.annotations.NotNull;

public class BungeePluginMetadata extends AbstractPluginMetadata {
    private final String pluginVersion;

    public BungeePluginMetadata(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public @NotNull String getVersion() { return pluginVersion; }
}

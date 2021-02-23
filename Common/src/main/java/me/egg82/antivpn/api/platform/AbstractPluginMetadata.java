package me.egg82.antivpn.api.platform;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractPluginMetadata implements PluginMetadata {
    private static final String API_VERSION = "2.0.0";

    public @NotNull String getApiVersion() { return API_VERSION; }
}

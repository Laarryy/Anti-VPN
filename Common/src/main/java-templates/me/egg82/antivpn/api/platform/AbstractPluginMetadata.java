package me.egg82.antivpn.api.platform;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractPluginMetadata implements PluginMetadata {
    private static final String API_VERSION = "${api.version}";

    @Override
    public @NotNull String getApiVersion() { return API_VERSION; }
}

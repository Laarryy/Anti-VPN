package me.egg82.antivpn.api.platform;

import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractPluginMetadata implements PluginMetadata {
    private static final String API_VERSION = "2.0.0";

    public @NonNull String getApiVersion() { return API_VERSION; }
}

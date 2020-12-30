package me.egg82.antivpn.api.platform;

public abstract class AbstractPluginMetadata implements PluginMetadata {
    private static final double API_VERSION = 1.0d;

    public double getApiVersion() { return API_VERSION; }
}

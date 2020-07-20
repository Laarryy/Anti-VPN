package me.egg82.antivpn.utils;

import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.VelocityBootstrap;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VelocityFakeClassLoader extends URLClassLoader {
    private final ProxyServer server;
    private final VelocityBootstrap plugin;

    public VelocityFakeClassLoader(ProxyServer server, VelocityBootstrap plugin) {
        super(new URL[0], VelocityBootstrap.class.getClassLoader());
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    protected void addURL(URL url) {
        Path path;
        try {
            path = Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to add URL to plugin classloader", e);
        }
        server.getPluginManager().addToClasspath(plugin, path);
    }
}

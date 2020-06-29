package me.egg82.antivpn.utils;

import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.VelocityBootstrap;

import java.net.URL;
import java.net.URLClassLoader;
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
        server.getPluginManager().addToClasspath(plugin, Paths.get(url.getPath()));
    }
}

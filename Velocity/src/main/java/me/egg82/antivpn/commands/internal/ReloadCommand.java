package me.egg82.antivpn.commands.internal;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.AntiVPN;
import me.egg82.antivpn.VelocityBootstrap;
import me.egg82.antivpn.utils.ConfigurationFileUtil;
import me.egg82.antivpn.utils.LogUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

public class ReloadCommand implements Runnable {
    private final AntiVPN concrete;
    private final VelocityBootstrap bootstrap;
    private final ProxyServer proxy;
    private final PluginDescription description;
    private final CommandSource source;

    public ReloadCommand(AntiVPN concrete, VelocityBootstrap bootstrap, ProxyServer proxy, PluginDescription description, CommandSource source) {
        this.concrete = concrete;
        this.bootstrap = bootstrap;
        this.proxy = proxy;
        this.description = description;
        this.source = source;
    }

    public void run() {
        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Reloading, please wait..").color(TextColor.YELLOW)).build());
        concrete.unloadServices();
        ConfigurationFileUtil.reloadConfig(bootstrap, proxy, description);
        concrete.loadServicesExternal();
        concrete.loadSQLExternal();
        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Configuration reloaded!").color(TextColor.GREEN)).build());
    }
}

package me.egg82.antivpn.commands.internal;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.AntiVPN;
import me.egg82.antivpn.VelocityBootstrap;
import me.egg82.antivpn.utils.ConfigurationFileUtil;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ServiceUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

public class ReloadCommand implements Runnable {
    private final Object plugin;
    private final ProxyServer proxy;
    private final PluginDescription description;
    private final CommandSource source;

    public ReloadCommand(Object plugin, ProxyServer proxy, PluginDescription description, CommandSource source) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.description = description;
        this.source = source;
    }

    public void run() {
        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Reloading, please wait..").color(TextColor.YELLOW)).build());

        ServiceUtil.unregisterWorkPool();
        ServiceUtil.unregisterRedis();
        ServiceUtil.unregisterRabbit();
        ServiceUtil.unregisterSQL();
        ConfigurationFileUtil.reloadConfig(plugin, proxy, description);
        ServiceUtil.registerWorkPool();
        ServiceUtil.registerRedis();
        ServiceUtil.registerRabbit();
        ServiceUtil.registerSQL();

        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Configuration reloaded!").color(TextColor.GREEN)).build());
    }
}

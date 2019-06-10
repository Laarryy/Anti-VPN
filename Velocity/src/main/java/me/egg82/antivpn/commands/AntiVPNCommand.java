package me.egg82.antivpn.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.AntiVPN;
import me.egg82.antivpn.VelocityBootstrap;
import me.egg82.antivpn.commands.internal.CheckCommand;
import me.egg82.antivpn.commands.internal.ReloadCommand;
import me.egg82.antivpn.commands.internal.ScoreCommand;
import me.egg82.antivpn.commands.internal.TestCommand;

@CommandAlias("antivpn|avpn")
public class AntiVPNCommand extends BaseCommand {
    private final Object plugin;
    private final ProxyServer proxy;
    private final PluginDescription pluginDescription;

    public AntiVPNCommand(Object plugin, ProxyServer proxy, PluginDescription description) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.pluginDescription = description;
    }

    @Subcommand("reload")
    @CommandPermission("avpn.admin")
    @Description("Reloads the plugin.")
    public void onReload(CommandSource source) {
        new ReloadCommand(plugin, proxy, pluginDescription, source).run();
    }

    @Subcommand("test")
    @CommandPermission("avpn.admin")
    @Description("Test an IP through the various (enabled) services. Note that this forces a check so will use credits every time it's run.")
    @Syntax("<ip>")
    public void onTest(CommandSource source, @Conditions("ip") String ip) {
        new TestCommand(source, ip).run();
    }

    @Subcommand("score")
    @CommandPermission("avpn.admin")
    @Description("Scores a particular source based on a pre-made list of known good and bad IPs. Note that this forces a check so will use credits every time it's run.")
    @Syntax("<source>")
    public void onScore(CommandSource source, @Conditions("source") String sourceName) {
        new ScoreCommand(source, sourceName).run();
    }

    @Subcommand("check")
    @CommandPermission("avpn.admin")
    @Description("Check an IP using the default system. This will return exactly the same value as any other API call.")
    @Syntax("<ip>")
    public void onCheck(CommandSource source, @Conditions("ip") String ip) {
        new CheckCommand(source, ip).run();
    }

    @CatchUnknown @Default
    @CommandCompletion("@subcommand")
    public void onDefault(CommandSource source, String[] args) {
        proxy.getCommandManager().execute(source, "antivpn help");
    }

    @HelpCommand
    @Syntax("[command]")
    public void onHelp(CommandSource source, CommandHelp help) { help.showHelp(); }
}

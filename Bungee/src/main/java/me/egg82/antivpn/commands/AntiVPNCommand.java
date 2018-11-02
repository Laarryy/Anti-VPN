package me.egg82.antivpn.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import me.egg82.antivpn.commands.internal.CheckCommand;
import me.egg82.antivpn.commands.internal.ReloadCommand;
import me.egg82.antivpn.commands.internal.ScoreCommand;
import me.egg82.antivpn.commands.internal.TestCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;

@CommandAlias("antivpn|avpn")
public class AntiVPNCommand extends BaseCommand {
    private final Plugin plugin;

    public AntiVPNCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Subcommand("reload")
    @CommandPermission("avpn.admin")
    @Description("Reloads the plugin.")
    public void onReload(CommandSender sender) {
        new ReloadCommand(plugin, sender).run();
    }

    @Subcommand("test")
    @CommandPermission("avpn.admin")
    @Description("Test an IP through the various (enabled) services. Note that this forces a check so will use credits every time it's run.")
    @Syntax("<ip>")
    public void onTest(CommandSender sender, @Conditions("ip") String ip) {
        new TestCommand(sender, ip).run();
    }

    @Subcommand("score")
    @CommandPermission("avpn.admin")
    @Description("Scores a particular source based on a pre-made list of known good and bad IPs. Note that this forces a check so will use credits every time it's run.")
    @Syntax("<source>")
    public void onScore(CommandSender sender, @Conditions("source") String source) {
        new ScoreCommand(sender, source).run();
    }

    @Subcommand("check")
    @CommandPermission("avpn.admin")
    @Description("Check an IP using the default system. This will return exactly the same value as any other API call.")
    @Syntax("<ip>")
    public void onCheck(CommandSender sender, @Conditions("ip") String ip) {
        new CheckCommand(sender, ip).run();
    }

    @HelpCommand
    @Syntax("[command]")
    public void onHelp(CommandSender sender, CommandHelp help) { help.showHelp(); }
}

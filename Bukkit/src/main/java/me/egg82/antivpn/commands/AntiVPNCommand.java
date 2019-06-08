package me.egg82.antivpn.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.taskchain.TaskChainFactory;
import me.egg82.antivpn.commands.internal.CheckCommand;
import me.egg82.antivpn.commands.internal.ReloadCommand;
import me.egg82.antivpn.commands.internal.ScoreCommand;
import me.egg82.antivpn.commands.internal.TestCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

@CommandAlias("antivpn|avpn")
public class AntiVPNCommand extends BaseCommand {
    private final Plugin plugin;
    private final TaskChainFactory taskFactory;

    public AntiVPNCommand(Plugin plugin, TaskChainFactory taskFactory) {
        this.plugin = plugin;
        this.taskFactory = taskFactory;
    }

    @Subcommand("reload")
    @CommandPermission("avpn.admin")
    @Description("Reloads the plugin.")
    public void onReload(CommandSender sender) {
        new ReloadCommand(plugin, taskFactory.newChain(), sender).run();
    }

    @Subcommand("test")
    @CommandPermission("avpn.admin")
    @Description("Test an IP through the various (enabled) services. Note that this forces a check so will use credits every time it's run.")
    @Syntax("<ip>")
    public void onTest(CommandSender sender, @Conditions("ip") String ip) {
        new TestCommand(taskFactory.newChain(), sender, ip).run();
    }

    @Subcommand("score")
    @CommandPermission("avpn.admin")
    @Description("Scores a particular source based on a pre-made list of known good and bad IPs. Note that this forces a check so will use credits every time it's run.")
    @Syntax("<source>")
    public void onScore(CommandSender sender, @Conditions("source") String source) {
        new ScoreCommand(taskFactory.newChain(), sender, source).run();
    }

    @Subcommand("check")
    @CommandPermission("avpn.admin")
    @Description("Check an IP using the default system. This will return exactly the same value as any other API call.")
    @Syntax("<ip>")
    public void onCheck(CommandSender sender, @Conditions("ip") String ip) {
        new CheckCommand(taskFactory.newChain(), sender, ip).run();
    }

    @CatchUnknown @Default
    @CommandCompletion("@subcommand")
    public void onDefault(CommandSender sender, String[] args) {
        Bukkit.getServer().dispatchCommand(sender, "antivpn help");
    }

    @HelpCommand
    @Syntax("[command]")
    public void onHelp(CommandSender sender, CommandHelp help) { help.showHelp(); }
}

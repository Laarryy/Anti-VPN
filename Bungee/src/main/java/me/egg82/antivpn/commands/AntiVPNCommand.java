package me.egg82.antivpn.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import me.egg82.antivpn.commands.internal.CheckCommand;
import me.egg82.antivpn.commands.internal.ImportCommand;
import me.egg82.antivpn.commands.internal.KickCommand;
import me.egg82.antivpn.commands.internal.ReloadCommand;
import me.egg82.antivpn.commands.internal.ScoreCommand;
import me.egg82.antivpn.commands.internal.TestCommand;
import me.egg82.antivpn.locale.LocalizedCommandSender;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

@CommandAlias("antivpn|avpn")
public class AntiVPNCommand extends BaseCommand {
    private final Plugin plugin;
    private final LocalizedCommandSender console;

    public AntiVPNCommand(@NotNull Plugin plugin, @NotNull LocalizedCommandSender console) {
        this.plugin = plugin;
        this.console = console;
    }

    @Subcommand("reload")
    @CommandPermission("avpn.admin")
    @Description("{@@description.reload}")
    public void onReload(@NotNull LocalizedCommandSender issuer) {
        new ReloadCommand(issuer, plugin.getDataFolder(), console).run();
    }

    @Subcommand("import")
    @CommandPermission("avpn.admin")
    @Description("{@@description.import}")
    @Syntax("<master> <slave> [batchSize]")
    @CommandCompletion("@storage @storage @nothing")
    public void onImport(@NotNull LocalizedCommandSender issuer, @NotNull @Conditions("storage") String master, @NotNull @Conditions("storage") String slave, @Default("50") String batchSize) {
        new ImportCommand(issuer, master, slave, batchSize).run();
    }

    @Subcommand("kick")
    @CommandPermission("avpn.admin")
    @Description("{@@description.kick}")
    @Syntax("<player> [type]")
    @CommandCompletion("@player @type")
    public void onKick(@NotNull LocalizedCommandSender issuer, @NotNull String player, @Default("vpn") String type) {
        new KickCommand(issuer, player, type).run();
    }

    @Subcommand("test")
    @CommandPermission("avpn.admin")
    @Description("{@@description.test}")
    @Syntax("<ip>")
    @CommandCompletion("@nothing")
    public void onTest(@NotNull LocalizedCommandSender issuer, @NotNull @Conditions("ip") String ip) {
        new TestCommand(issuer, ip).run();
    }

    @Subcommand("score")
    @CommandPermission("avpn.admin")
    @Description("{@@description.score}")
    @Syntax("<source>")
    @CommandCompletion("@source @nothing")
    public void onScore(@NotNull LocalizedCommandSender issuer, @NotNull @Conditions("source") String source) {
        new ScoreCommand(issuer, source).run();
    }

    @Subcommand("check")
    @CommandPermission("avpn.admin")
    @Description("{@@description.check}")
    @Syntax("<ip|player>")
    @CommandCompletion("@player @nothing")
    public void onCheck(@NotNull LocalizedCommandSender issuer, @NotNull String type) {
        new CheckCommand(issuer, type).run();
    }

    @CatchUnknown @Default
    @CommandCompletion("@subcommand")
    public void onDefault(@NotNull CommandSender sender, String[] args) {
        ProxyServer.getInstance().getPluginManager().dispatchCommand(sender, "antivpn help");
    }

    @HelpCommand
    @Syntax("[command]")
    public void onHelp(@NotNull CommandSender sender, @NotNull CommandHelp help) { help.showHelp(); }
}

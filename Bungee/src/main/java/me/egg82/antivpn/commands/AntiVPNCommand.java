package me.egg82.antivpn.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import me.egg82.antivpn.commands.internal.*;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

@CommandAlias("antivpn|avpn")
public class AntiVPNCommand extends BaseCommand {
    private final Plugin plugin;
    private final CommandIssuer console;

    public AntiVPNCommand(@NonNull Plugin plugin, @NonNull CommandIssuer console) {
        this.plugin = plugin;
        this.console = console;
    }

    @Subcommand("reload")
    @CommandPermission("avpn.admin")
    @Description("{@@description.reload}")
    public void onReload(@NonNull CommandIssuer issuer) {
        new ReloadCommand(issuer, plugin.getDataFolder(), console).run();
    }

    @Subcommand("import")
    @CommandPermission("avpn.admin")
    @Description("{@@description.import}")
    @Syntax("<master> <slave> [batchSize]")
    @CommandCompletion("@storage @storage @nothing")
    public void onImport(@NonNull CommandIssuer issuer, @NonNull @Conditions("storage") String master, @NonNull @Conditions("storage") String slave, @Default("50") String batchSize) {
        new ImportCommand(issuer, master, slave, batchSize).run();
    }

    @Subcommand("kick")
    @CommandPermission("avpn.admin")
    @Description("{@@description.kick}")
    @Syntax("<player> [type]")
    @CommandCompletion("@player @type")
    public void onKick(@NonNull CommandIssuer issuer, @NonNull String player, @Default("vpn") String type) {
        new KickCommand(issuer, player, type).run();
    }

    @Subcommand("test")
    @CommandPermission("avpn.admin")
    @Description("{@@description.test}")
    @Syntax("<ip>")
    @CommandCompletion("@nothing")
    public void onTest(@NonNull CommandIssuer issuer, @NonNull @Conditions("ip") String ip) {
        new TestCommand(issuer, ip).run();
    }

    @Subcommand("score")
    @CommandPermission("avpn.admin")
    @Description("{@@description.score}")
    @Syntax("<source>")
    @CommandCompletion("@source @nothing")
    public void onScore(@NonNull CommandIssuer issuer, @NonNull @Conditions("source") String source) {
        new ScoreCommand(issuer, source).run();
    }

    @Subcommand("check")
    @CommandPermission("avpn.admin")
    @Description("{@@description.check}")
    @Syntax("<ip|player>")
    @CommandCompletion("@player @nothing")
    public void onCheck(@NonNull CommandIssuer issuer, @NonNull String type) {
        new CheckCommand(issuer, type).run();
    }

    @CatchUnknown @Default
    @CommandCompletion("@subcommand")
    public void onDefault(@NonNull CommandSender sender, String[] args) {
        ProxyServer.getInstance().getPluginManager().dispatchCommand(sender, "antivpn help");
    }

    @HelpCommand
    @Syntax("[command]")
    public void onHelp(@NonNull CommandSender sender, @NonNull CommandHelp help) { help.showHelp(); }
}

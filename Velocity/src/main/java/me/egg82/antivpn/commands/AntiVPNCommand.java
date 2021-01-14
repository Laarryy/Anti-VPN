package me.egg82.antivpn.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.File;
import me.egg82.antivpn.commands.internal.*;
import org.checkerframework.checker.nullness.qual.NonNull;

@CommandAlias("antivpn|avpn")
public class AntiVPNCommand extends BaseCommand {
    private final ProxyServer proxy;
    private final PluginDescription description;
    private final CommandIssuer console;

    public AntiVPNCommand(@NonNull ProxyServer proxy, @NonNull PluginDescription description, @NonNull CommandIssuer console) {
        this.proxy = proxy;
        this.description = description;
        this.console = console;
    }

    @Subcommand("reload")
    @CommandPermission("avpn.admin")
    @Description("{@@description.reload}")
    public void onReload(@NonNull CommandIssuer issuer) {
        new ReloadCommand(proxy, issuer, new File(description.getSource().get().getParent().toFile(), description.getName().get()), console).run();
    }

    @Subcommand("import")
    @CommandPermission("avpn.admin")
    @Description("{@@description.import}")
    @Syntax("<master> <slave> [batchSize]")
    @CommandCompletion("@storage @storage @nothing")
    public void onImport(@NonNull CommandIssuer issuer, @NonNull @Conditions("storage") String master, @NonNull @Conditions("storage") String slave, @Default("50") String batchSize) {
        new ImportCommand(proxy, issuer, master, slave, batchSize).run();
    }

    @Subcommand("kick")
    @CommandPermission("avpn.admin")
    @Description("{@@description.kick}")
    @Syntax("<player> [type]")
    @CommandCompletion("@player @type")
    public void onKick(@NonNull CommandIssuer issuer, @NonNull String player, @Default("vpn") String type) {
        new KickCommand(proxy, issuer, player, type).run();
    }

    @Subcommand("test")
    @CommandPermission("avpn.admin")
    @Description("{@@description.test}")
    @Syntax("<ip>")
    @CommandCompletion("@nothing")
    public void onTest(@NonNull CommandIssuer issuer, @NonNull @Conditions("ip") String ip) {
        new TestCommand(proxy, issuer, ip).run();
    }

    @Subcommand("score")
    @CommandPermission("avpn.admin")
    @Description("{@@description.score}")
    @Syntax("<source>")
    @CommandCompletion("@source @nothing")
    public void onScore(@NonNull CommandIssuer issuer, @NonNull @Conditions("source") String source) {
        new ScoreCommand(proxy, issuer, source).run();
    }

    @Subcommand("check")
    @CommandPermission("avpn.admin")
    @Description("{@@description.check}")
    @Syntax("<ip|player>")
    @CommandCompletion("@player @nothing")
    public void onCheck(@NonNull CommandIssuer issuer, @NonNull String type) {
        new CheckCommand(proxy, issuer, type).run();
    }

    @CatchUnknown @Default
    @CommandCompletion("@subcommand")
    public void onDefault(@NonNull CommandSource source, String[] args) {
        proxy.getCommandManager().executeImmediatelyAsync(source, "antivpn help");
    }

    @HelpCommand
    @Syntax("[command]")
    public void onHelp(@NonNull CommandSource source, @NonNull CommandHelp help) { help.showHelp(); }
}

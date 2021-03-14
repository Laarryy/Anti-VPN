package me.egg82.antivpn.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.commands.internal.*;
import me.egg82.antivpn.locale.LocalizedCommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@CommandAlias("antivpn|avpn")
public class AntiVPNCommand extends BaseCommand {
    private final ProxyServer proxy;
    private final PluginDescription description;
    private final LocalizedCommandSender console;

    public AntiVPNCommand(@NotNull ProxyServer proxy, @NotNull PluginDescription description, @NotNull LocalizedCommandSender console) {
        this.proxy = proxy;
        this.description = description;
        this.console = console;
    }

    @Subcommand("reload")
    @CommandPermission("avpn.admin")
    @Description("{@@description.reload}")
    public void onReload(@NotNull LocalizedCommandSender issuer) {
        new ReloadCommand(proxy, issuer, new File(description.getSource().get().getParent().toFile(), description.getName().get()), console).run();
    }

    @Subcommand("import")
    @CommandPermission("avpn.admin")
    @Description("{@@description.import}")
    @Syntax("<master> <slave> [batchSize]")
    @CommandCompletion("@storage @storage @nothing")
    public void onImport(
            @NotNull LocalizedCommandSender issuer,
            @NotNull @Conditions("storage") String master,
            @NotNull @Conditions("storage") String slave,
            @Default("50") String batchSize
    ) {
        new ImportCommand(proxy, issuer, master, slave, batchSize).run();
    }

    @Subcommand("kick")
    @CommandPermission("avpn.admin")
    @Description("{@@description.kick}")
    @Syntax("<player> [type]")
    @CommandCompletion("@player @type")
    public void onKick(@NotNull LocalizedCommandSender issuer, @NotNull String player, @Default("vpn") String type) {
        new KickCommand(proxy, issuer, player, type).run();
    }

    @Subcommand("test")
    @CommandPermission("avpn.admin")
    @Description("{@@description.test}")
    @Syntax("<ip>")
    @CommandCompletion("@nothing")
    public void onTest(@NotNull LocalizedCommandSender issuer, @NotNull @Conditions("ip") String ip) {
        new TestCommand(proxy, issuer, ip).run();
    }

    @Subcommand("score")
    @CommandPermission("avpn.admin")
    @Description("{@@description.score}")
    @Syntax("<source>")
    @CommandCompletion("@source @nothing")
    public void onScore(@NotNull LocalizedCommandSender issuer, @NotNull @Conditions("source") String source) {
        new ScoreCommand(proxy, issuer, source).run();
    }

    @Subcommand("check")
    @CommandPermission("avpn.admin")
    @Description("{@@description.check}")
    @Syntax("<ip|player>")
    @CommandCompletion("@player @nothing")
    public void onCheck(@NotNull LocalizedCommandSender issuer, @NotNull String type) {
        new CheckCommand(proxy, issuer, type).run();
    }

    @CatchUnknown
    @Default
    @CommandCompletion("@subcommand")
    public void onDefault(@NotNull CommandSource source, String[] args) {
        proxy.getCommandManager().executeImmediatelyAsync(source, "antivpn help");
    }

    @HelpCommand
    @Syntax("[command]")
    public void onHelp(@NotNull CommandSource source, @NotNull CommandHelp help) {
        help.showHelp();
    }
}

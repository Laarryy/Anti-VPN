package me.egg82.antivpn.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.commands.internal.*;
import me.egg82.antivpn.services.StorageMessagingHandler;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CommandAlias("antivpn|avpn")
public class AntiVPNCommand extends BaseCommand {
    private final Logger logger = LoggerFactory.getLogger(getClass());

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
    @Description("{@@description.reload}")
    public void onReload(CommandIssuer issuer) {
        StorageMessagingHandler handler;
        try {
            handler = ServiceLocator.get(StorageMessagingHandler.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }
        new ReloadCommand(plugin, proxy, pluginDescription, handler, issuer).run();
    }

    @Subcommand("import")
    @CommandPermission("avpn.admin")
    @Description("{@@description.import}")
    @Syntax("<master> <slave> [batchSize]")
    @CommandCompletion("@storage @storage @nothing")
    public void onImport(CommandIssuer issuer, @Conditions("storage") String master, @Conditions("storage") String slave, @Default("50") String batchSize) {
        new ImportCommand(issuer, master, slave, batchSize).run();
    }

    @Subcommand("test")
    @CommandPermission("avpn.admin")
    @Description("{@@description.test}")
    @Syntax("<ip>")
    @CommandCompletion("@nothing")
    public void onTest(CommandIssuer issuer, @Conditions("ip") String ip) {
        new TestCommand(issuer, ip).run();
    }

    @Subcommand("score")
    @CommandPermission("avpn.admin")
    @Description("{@@description.score}")
    @Syntax("<source>")
    @CommandCompletion("@source @nothing")
    public void onScore(CommandIssuer issuer, @Conditions("source") String source) {
        new ScoreCommand(issuer, source).run();
    }

    @Subcommand("check")
    @CommandPermission("avpn.admin")
    @Description("{@@description.check}")
    @Syntax("<ip|player>")
    @CommandCompletion("@player @nothing")
    public void onCheck(CommandIssuer issuer, String type) {
        new CheckCommand(issuer, proxy, type).run();
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

package me.egg82.antivpn.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import co.aikar.taskchain.TaskChainFactory;
import me.egg82.antivpn.commands.internal.*;
import me.egg82.antivpn.services.StorageMessagingHandler;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CommandAlias("antivpn|avpn")
public class AntiVPNCommand extends BaseCommand {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Plugin plugin;
    private final TaskChainFactory taskFactory;

    public AntiVPNCommand(Plugin plugin, TaskChainFactory taskFactory) {
        this.plugin = plugin;
        this.taskFactory = taskFactory;
    }

    @Subcommand("reload")
    @CommandPermission("avpn.admin")
    @Description("{@@description.reload}")
    public void onReload(CommandSender sender) {
        StorageMessagingHandler handler;
        try {
            handler = ServiceLocator.get(StorageMessagingHandler.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }
        new ReloadCommand(plugin, taskFactory.newChain(), handler, issuer).run();
    }

    @Subcommand("import")
    @CommandPermission("avpn.admin")
    @Description("{@@description.import}")
    @Syntax("<master> <slave> [batchSize]")
    @CommandCompletion("@storage @storage @nothing")
    public void onImport(CommandIssuer issuer, @Conditions("storage") String master, @Conditions("storage") String slave, @Default("50") String batchSize) {
        new ImportCommand(issuer, master, slave, batchSize, taskFactory.newChain()).run();
    }

    @Subcommand("test")
    @CommandPermission("avpn.admin")
    @Description("{@@description.test}")
    @Syntax("<ip>")
    @CommandCompletion("@ip @nothing")
    public void onTest(CommandSender sender, @Conditions("ip") String ip) {
        new TestCommand(taskFactory.newChain(), sender, ip).run();
    }

    @Subcommand("score")
    @CommandPermission("avpn.admin")
    @Description("{@@description.score}")
    @Syntax("<source>")
    @CommandCompletion("@source @nothing")
    public void onScore(CommandSender sender, @Conditions("source") String source) {
        new ScoreCommand(taskFactory.newChain(), sender, source).run();
    }

    @Subcommand("check")
    @CommandPermission("avpn.admin")
    @Description("{@@description.check}")
    @Syntax("<ip|player>")
    @CommandCompletion("@ip|@player @nothing")
    public void onCheck(CommandSender sender, String type) {
        new CheckCommand(taskFactory.newChain(), sender, type).run();
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

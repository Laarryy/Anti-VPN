package me.egg82.antivpn.commands;

import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
import me.egg82.antivpn.lang.BukkitLocaleCommandUtil;
import me.egg82.antivpn.lang.BukkitLocalizedCommandSender;
import me.egg82.antivpn.logging.GELFLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CommandHolder {
    protected final Logger logger = new GELFLogger(LoggerFactory.getLogger(getClass()));

    protected final List<Command<BukkitLocalizedCommandSender>> commands = new ArrayList<>();

    protected CommandHolder() { }

    public final int numCommands() { return commands.size(); }

    public final void cancel() {
        PaperCommandManager<BukkitLocalizedCommandSender> commandManager = BukkitLocaleCommandUtil.getCommandManager();
        for (Command<BukkitLocalizedCommandSender> command : commands) {
            // TODO: Deregister command
        }
    }

    protected final void registerAll() {
        PaperCommandManager<BukkitLocalizedCommandSender> commandManager = BukkitLocaleCommandUtil.getCommandManager();
        for (Command<BukkitLocalizedCommandSender> command : commands) {
            commandManager.command(command);
        }
    }
}

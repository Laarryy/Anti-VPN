package me.egg82.antivpn.commands.internal;

import cloud.commandframework.execution.CommandExecutionHandler;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.lang.BukkitLocalizedCommandSender;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCommand implements CommandExecutionHandler<BukkitLocalizedCommandSender> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final PaperCommandManager<BukkitLocalizedCommandSender> commandManager;

    protected AbstractCommand(@NotNull PaperCommandManager<BukkitLocalizedCommandSender> commandManager) {
        this.commandManager = commandManager;
    }

    protected @NotNull CompletableFuture<@NotNull UUID> fetchUuid(@NotNull String name) { return PlayerLookup.get(name).thenApply(PlayerInfo::getUUID); }
}

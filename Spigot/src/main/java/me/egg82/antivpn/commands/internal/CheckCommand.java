package me.egg82.antivpn.commands.internal;

import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.context.CommandContext;
import me.egg82.antivpn.locale.BukkitLocalizedCommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

public class CheckCommand extends AbstractCommand {
    public CheckCommand(@NotNull BukkitCommandManager<BukkitLocalizedCommandSender> commandManager) {
        super(commandManager);
    }

    @Override
    public void execute(@NonNull CommandContext<BukkitLocalizedCommandSender> commandContext) { }
    /*private final String type;

    public CheckCommand(@NotNull CommandIssuer issuer, @NotNull TaskChainFactory taskFactory, @NotNull String type) {
        super(issuer, taskFactory);
        this.type = type;
    }

    public void run() {
        issuer.sendInfo(MessageKey.CHECK__BEGIN, "{type}", type);

        if (ValidationUtil.isValidIp(type)) {
            checkIp(type);
        } else {
            checkPlayer(type);
        }
    }

    private void checkIp(@NotNull String ip) {
        IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();

        if (ipManager.getCurrentAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
            TaskChain<Void> chain = taskFactory.newChain();
            chain.setErrorHandler((ex, task) -> ExceptionUtil.handleException(ex, logger));
            chain
                .asyncFirstFuture(() -> ipManager.consensus(ip, true))
                .abortIfNull(this.handleAbort)
                .syncLast(v -> issuer.sendInfo(v >= ipManager.getMinConsensusValue() ? MessageKey.CHECK__VPN_DETECTED : MessageKey.CHECK__NO_VPN_DETECTED))
                .execute();
        } else {
            TaskChain<Void> chain = taskFactory.newChain();
            chain.setErrorHandler((ex, task) -> ExceptionUtil.handleException(ex, logger));
            chain
                .asyncFirstFuture(() -> ipManager.cascade(ip, true))
                .abortIfNull(this.handleAbort)
                .syncLast(v -> issuer.sendInfo(Boolean.TRUE.equals(v) ? MessageKey.CHECK__VPN_DETECTED : MessageKey.CHECK__NO_VPN_DETECTED))
                .execute();
        }
    }

    private void checkPlayer(@NotNull String playerName) {
        PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();

        TaskChain<Void> chain = taskFactory.newChain();
        chain.setErrorHandler((ex, task) -> ExceptionUtil.handleException(ex, logger));
        chain
            .asyncFirstFuture(() -> fetchUuid(playerName))
            .abortIfNull(this.handleAbort)
            .asyncFuture(v -> playerManager.checkMcLeaks(v, true))
            .abortIfNull(this.handleAbort)
            .syncLast(v -> issuer.sendInfo(Boolean.TRUE.equals(v) ? MessageKey.CHECK__MCLEAKS_DETECTED : MessageKey.CHECK__NO_MCLEAKS_DETECTED))
            .execute();
    }*/
}

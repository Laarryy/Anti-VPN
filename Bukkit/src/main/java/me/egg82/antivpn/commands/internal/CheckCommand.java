package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.utils.ExceptionUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CheckCommand extends AbstractCommand {
    private final String type;

    public CheckCommand(@NonNull CommandIssuer issuer, @NonNull TaskChainFactory taskFactory, @NonNull String type) {
        super(issuer, taskFactory);
        this.type = type;
    }

    public void run() {
        issuer.sendInfo(Message.CHECK__BEGIN, "{type}", type);

        if (ValidationUtil.isValidIp(type)) {
            checkIp(type);
        } else {
            checkPlayer(type);
        }
    }

    private void checkIp(@NonNull String ip) {
        IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();

        if (ipManager.getCurrentAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
            TaskChain<Void> chain = taskFactory.newChain();
            chain.setErrorHandler((ex, task) -> ExceptionUtil.handleException(ex, logger));
            chain
                .asyncFirstFuture(() -> ipManager.consensus(ip, true))
                .abortIfNull(this.handleAbort)
                .syncLast(v -> issuer.sendInfo(v >= ipManager.getMinConsensusValue() ? Message.CHECK__VPN_DETECTED : Message.CHECK__NO_VPN_DETECTED))
                .execute();
        } else {
            TaskChain<Void> chain = taskFactory.newChain();
            chain.setErrorHandler((ex, task) -> ExceptionUtil.handleException(ex, logger));
            chain
                .asyncFirstFuture(() -> ipManager.cascade(ip, true))
                .abortIfNull(this.handleAbort)
                .syncLast(v -> issuer.sendInfo(Boolean.TRUE.equals(v) ? Message.CHECK__VPN_DETECTED : Message.CHECK__NO_VPN_DETECTED))
                .execute();
        }
    }

    private void checkPlayer(@NonNull String playerName) {
        PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();

        TaskChain<Void> chain = taskFactory.newChain();
        chain.setErrorHandler((ex, task) -> ExceptionUtil.handleException(ex, logger));
        chain
            .asyncFirstFuture(() -> fetchUuid(playerName))
            .abortIfNull(this.handleAbort)
            .asyncFuture(v -> playerManager.checkMcLeaks(v, true))
            .abortIfNull(this.handleAbort)
            .syncLast(v -> issuer.sendInfo(Boolean.TRUE.equals(v) ? Message.CHECK__MCLEAKS_DETECTED : Message.CHECK__NO_MCLEAKS_DETECTED))
            .execute();
    }
}

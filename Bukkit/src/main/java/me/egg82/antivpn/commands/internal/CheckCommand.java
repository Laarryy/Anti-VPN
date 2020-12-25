package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChainFactory;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.utils.ValidationUtil;

public class CheckCommand extends AbstractCommand {
    private final String type;

    private final VPNAPI api = VPNAPIProvider.getInstance();

    public CheckCommand(CommandIssuer issuer, TaskChainFactory taskFactory, String type) {
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

    private void checkIp(String ip) {
        taskFactory.<Void>newChain()
                .<Boolean>asyncCallback((v, r) -> {
                    if (api.getIpManager().getCurrentAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
                        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
                        if (cachedConfig == null) {
                            logger.error("Cached config could not be fetched.");
                            r.accept(null);
                            return;
                        }

                        try {
                            r.accept(api.getIpManager().consensus(ip, true)
                                    .exceptionally(this::handleException)
                                    .join() >= cachedConfig.getVPNAlgorithmConsensus());
                            return;
                        } catch (CompletionException ignored) { }
                    } else {
                        try {
                            r.accept(api.getIpManager().cascade(ip, true)
                                    .exceptionally(this::handleException)
                                    .join());
                            return;
                        } catch (CompletionException ignored) { }
                    }

                    r.accept(null);
                })
                .abortIfNull(this.handleAbort)
                .syncLast(v -> issuer.sendInfo(Boolean.TRUE.equals(v) ? Message.CHECK__VPN_DETECTED : Message.CHECK__NO_VPN_DETECTED))
                .execute();
    }

    private void checkPlayer(String playerName) {
        taskFactory.<Void>newChain()
                .<UUID>asyncCallback((v, r) -> r.accept(fetchUuid(playerName)))
                .abortIfNull(this.handleAbort)
                .<Boolean>asyncCallback((v, r) -> {
                    try {
                        r.accept(api.getPlayerManager().checkMcLeaks(v, true)
                                .exceptionally(this::handleException)
                                .join());
                        return;
                    } catch (CompletionException ignored) { }

                    r.accept(null);
                })
                .abortIfNull(this.handleAbort)
                .syncLast(v -> issuer.sendInfo(Boolean.TRUE.equals(v) ? Message.CHECK__MCLEAKS_DETECTED : Message.CHECK__NO_MCLEAKS_DETECTED))
                .execute();
    }
}

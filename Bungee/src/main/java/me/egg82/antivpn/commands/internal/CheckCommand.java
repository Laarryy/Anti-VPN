package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import java.util.UUID;
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

    public CheckCommand(@NonNull CommandIssuer issuer, @NonNull String type) {
        super(issuer);
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
            ipManager.consensus(ip, true).whenCompleteAsync((val, ex) -> {
                if (ex != null) {
                    ExceptionUtil.handleException(ex, logger);
                    issuer.sendError(Message.ERROR__INTERNAL);
                    return;
                }
                issuer.sendInfo(val != null && val >= ipManager.getMinConsensusValue() ? Message.CHECK__VPN_DETECTED : Message.CHECK__NO_VPN_DETECTED);
            });
        } else {
            ipManager.cascade(ip, true).whenCompleteAsync((val, ex) -> {
                if (ex != null) {
                    ExceptionUtil.handleException(ex, logger);
                    issuer.sendError(Message.ERROR__INTERNAL);
                    return;
                }
                issuer.sendInfo(Boolean.TRUE.equals(val) ? Message.CHECK__VPN_DETECTED : Message.CHECK__NO_VPN_DETECTED);
            });
        }
    }

    private void checkPlayer(@NonNull String playerName) {
        PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();

        UUID uuid = fetchUuid(playerName);
        if (uuid == null) {
            issuer.sendError(Message.ERROR__INTERNAL);
            return;
        }

        playerManager.checkMcLeaks(uuid, true).whenCompleteAsync((val, ex) -> {
            if (ex != null) {
                ExceptionUtil.handleException(ex, logger);
                issuer.sendError(Message.ERROR__INTERNAL);
                return;
            }
            issuer.sendInfo(Boolean.TRUE.equals(val) ? Message.CHECK__MCLEAKS_DETECTED : Message.CHECK__NO_MCLEAKS_DETECTED);
        });
    }
}

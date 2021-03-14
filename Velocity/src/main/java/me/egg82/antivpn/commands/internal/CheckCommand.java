package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.utils.ExceptionUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import org.jetbrains.annotations.NotNull;

public class CheckCommand extends AbstractCommand {
    private final String type;

    public CheckCommand(@NotNull ProxyServer proxy, @NotNull CommandIssuer issuer, @NotNull String type) {
        super(proxy, issuer);
        this.type = type;
    }

    @Override
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
            ipManager.consensus(ip, true).whenCompleteAsync((val, ex) -> {
                if (ex != null) {
                    ExceptionUtil.handleException(ex, logger);
                    issuer.sendError(MessageKey.ERROR__INTERNAL);
                    return;
                }
                issuer.sendInfo(val != null && val >= ipManager.getMinConsensusValue() ? MessageKey.CHECK__VPN_DETECTED : MessageKey.CHECK__NO_VPN_DETECTED);
            });
        } else {
            ipManager.cascade(ip, true).whenCompleteAsync((val, ex) -> {
                if (ex != null) {
                    ExceptionUtil.handleException(ex, logger);
                    issuer.sendError(MessageKey.ERROR__INTERNAL);
                    return;
                }
                issuer.sendInfo(Boolean.TRUE.equals(val) ? MessageKey.CHECK__VPN_DETECTED : MessageKey.CHECK__NO_VPN_DETECTED);
            });
        }
    }

    private void checkPlayer(@NotNull String playerName) {
        PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();

        fetchUuid(playerName)
                .thenComposeAsync(uuid -> playerManager.checkMcLeaks(uuid, true))
                .whenCompleteAsync((val, ex) -> {
                    if (ex != null) {
                        ExceptionUtil.handleException(ex, logger);
                        issuer.sendError(MessageKey.ERROR__INTERNAL);
                        return;
                    }
                    issuer.sendInfo(Boolean.TRUE.equals(val) ? MessageKey.CHECK__MCLEAKS_DETECTED : MessageKey.CHECK__NO_MCLEAKS_DETECTED);
                });
    }
}

package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.utils.ValidationUtil;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CheckCommand extends AbstractCommand {
    private final String type;

    public CheckCommand(@NonNull ProxyServer proxy, @NonNull CommandIssuer issuer, @NonNull String type) {
        super(proxy, issuer);
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
        IPManager ipManager = VPNAPIProvider.getInstance().getIpManager();

        if (ipManager.getCurrentAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
            try {
                issuer.sendInfo(ipManager.consensus(ip, true)
                    .exceptionally(this::handleException)
                    .join() >= ipManager.getMinConsensusValue() ? Message.CHECK__VPN_DETECTED : Message.CHECK__NO_VPN_DETECTED);
                return;
            } catch (CompletionException ignored) { }
            catch (Exception ex) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.error(ex.getMessage(), ex);
                } else {
                    logger.error(ex.getMessage());
                }
            }
        } else {
            try {
                issuer.sendInfo(ipManager.cascade(ip, true)
                    .exceptionally(this::handleException)
                    .join() ? Message.CHECK__VPN_DETECTED : Message.CHECK__NO_VPN_DETECTED);
                return;
            } catch (CompletionException ignored) { }
            catch (Exception ex) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.error(ex.getMessage(), ex);
                } else {
                    logger.error(ex.getMessage());
                }
            }
        }

        issuer.sendError(Message.ERROR__INTERNAL);
    }

    private void checkPlayer(@NonNull String playerName) {
        PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();

        UUID uuid = fetchUuid(playerName);
        if (uuid == null) {
            issuer.sendError(Message.ERROR__INTERNAL);
            return;
        }

        try {
            issuer.sendInfo(playerManager.checkMcLeaks(uuid, true)
                .exceptionally(this::handleException)
                .join() ? Message.CHECK__MCLEAKS_DETECTED : Message.CHECK__NO_MCLEAKS_DETECTED);
            return;
        } catch (CompletionException ignored) { }
        catch (Exception ex) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.error(ex.getMessage(), ex);
            } else {
                logger.error(ex.getMessage());
            }
        }

        issuer.sendError(Message.ERROR__INTERNAL);
    }
}

package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import me.egg82.antivpn.utils.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommandIssuer issuer;
    private final String type;

    private final VPNAPI api = VPNAPI.getInstance();

    public CheckCommand(CommandIssuer issuer, String type) {
        this.issuer = issuer;
        this.type = type;
    }

    public void run() {
        issuer.sendInfo(Message.CHECK__BEGIN, "{type}", type);

        if (ValidationUtil.isValidIp(type)) {
            Optional<CachedConfig> cachedConfig = ConfigUtil.getCachedConfig();
            if (!cachedConfig.isPresent()) {
                logger.error("Cached config could not be fetched.");
                issuer.sendError(Message.ERROR__INTERNAL);
                return;
            }

            if (cachedConfig.get().getVPNAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
                try {
                    issuer.sendInfo(api.consensus(type) >= cachedConfig.get().getVPNAlgorithmConsensus() ? Message.CHECK__VPN_DETECTED : Message.CHECK__NO_VPN_DETECTED);
                    return;
                } catch (APIException ex) {
                    if (cachedConfig.get().getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
                }
            } else {
                try {
                    issuer.sendInfo(api.cascade(type) ? Message.CHECK__VPN_DETECTED : Message.CHECK__NO_VPN_DETECTED);
                    return;
                } catch (APIException ex) {
                    if (cachedConfig.get().getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
                }
            }
            issuer.sendError(Message.ERROR__INTERNAL);
        } else {
            UUID playerID = getPlayerUUID(type);
            if (playerID == null) {
                issuer.sendError(Message.ERROR__INTERNAL);
                return;
            }

            Optional<CachedConfig> cachedConfig = ConfigUtil.getCachedConfig();
            if (!cachedConfig.isPresent()) {
                logger.error("Cached config could not be fetched.");
                issuer.sendError(Message.ERROR__INTERNAL);
                return;
            }

            try {
                issuer.sendInfo(api.isMCLeaks(playerID) ? Message.CHECK__MCLEAKS_DETECTED : Message.CHECK__NO_MCLEAKS_DETECTED);
                return;
            } catch (APIException ex) {
                if (cachedConfig.get().getDebug()) {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                }
            }
            issuer.sendError(Message.ERROR__INTERNAL);
        }
    }

    private UUID getPlayerUUID(String name) {
        PlayerInfo info;
        try {
            info = PlayerLookup.get(name);
        } catch (IOException ex) {
            logger.warn("Could not fetch player UUID. (rate-limited?)", ex);
            return null;
        }
        return info.getUUID();
    }
}

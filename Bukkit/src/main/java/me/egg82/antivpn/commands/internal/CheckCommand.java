package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainAbortAction;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.enums.Message;
import me.egg82.antivpn.enums.VPNAlgorithmMethod;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class CheckCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommandIssuer issuer;
    private final String type;
    private final TaskChain<?> chain;

    private final VPNAPI api = VPNAPI.getInstance();

    public CheckCommand(CommandIssuer issuer, String type, TaskChain<?> chain) {
        this.issuer = issuer;
        this.type = type;
        this.chain = chain;
    }

    public void run() {
        issuer.sendInfo(Message.CHECK__BEGIN, "{type}", type);

        if (ValidationUtil.isValidIp(type)) {
            chain
                    .<Optional<Boolean>>asyncCallback((v, f) -> {
                        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
                        if (!cachedConfig.isPresent()) {
                            logger.error("Cached config could not be fetched.");
                            f.accept(Optional.empty());
                            return;
                        }

                        if (cachedConfig.get().getVPNAlgorithmMethod() == VPNAlgorithmMethod.CONSESNSUS) {
                            try {
                                f.accept(Optional.of(api.consensus(type) >= cachedConfig.get().getVPNAlgorithmConsensus()));
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
                                f.accept(Optional.of(api.cascade(type)));
                                return;
                            } catch (APIException ex) {
                                if (cachedConfig.get().getDebug()) {
                                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                                } else {
                                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                                }
                            }
                        }
                        f.accept(Optional.empty());
                    })
                    .syncLast(f -> {
                        if (!f.isPresent()) {
                            issuer.sendError(Message.ERROR__INTERNAL);
                            return;
                        }
                        issuer.sendInfo(f.get() ? Message.CHECK__VPN_DETECTED : Message.CHECK__NO_VPN_DETECTED);
                    })
                    .execute();
        } else {
            chain
                    .<UUID>asyncCallback((v, f) -> f.accept(getPlayerUUID(type)))
                    .abortIfNull(new TaskChainAbortAction<Object, Object, Object>() {
                        public void onAbort(TaskChain<?> chain, Object arg1, Object arg2, Object arg3) {
                            issuer.sendError(Message.ERROR__INTERNAL);
                        }
                    })
                    .<Optional<Boolean>>asyncCallback((v, f) -> {
                        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
                        if (!cachedConfig.isPresent()) {
                            logger.error("Cached config could not be fetched.");
                            f.accept(Optional.empty());
                            return;
                        }

                        try {
                            f.accept(Optional.of(api.isMCLeaks(v)));
                            return;
                        } catch (APIException ex) {
                            if (cachedConfig.get().getDebug()) {
                                logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                            } else {
                                logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                            }
                        }
                        f.accept(Optional.empty());
                    })
                    .syncLast(f -> {
                        if (!f.isPresent()) {
                            issuer.sendError(Message.ERROR__INTERNAL);
                            return;
                        }
                        issuer.sendInfo(f.get() ? Message.CHECK__MCLEAKS_DETECTED : Message.CHECK__NO_MCLEAKS_DETECTED);
                    })
                    .execute();
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

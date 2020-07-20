package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.enums.Message;
import me.egg82.antivpn.utils.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class TestCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommandIssuer issuer;
    private final String ip;

    private final VPNAPI api = VPNAPI.getInstance();

    public TestCommand(CommandIssuer issuer, String ip) {
        this.issuer = issuer;
        this.ip = ip;
    }

    public void run() {
        issuer.sendInfo(Message.TEST__BEGIN, "{ip}", ip);

        try {
            Map<String, Optional<Boolean>> map = api.testAllSources(ip);
            for (Map.Entry<String, Optional<Boolean>> kvp : map.entrySet()) {
                if (!kvp.getValue().isPresent()) {
                    issuer.sendInfo(Message.TEST__ERROR, "{source}", kvp.getKey());
                    continue;
                }
                issuer.sendInfo(kvp.getValue().get() ? Message.TEST__VPN_DETECTED : Message.TEST__NO_VPN_DETECTED, "{source}", kvp.getKey());
            }
            issuer.sendInfo(Message.TEST__END, "{ip}", ip);
            return;
        } catch (APIException ex) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
            } else {
                logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
            }
        }
        issuer.sendError(Message.ERROR__INTERNAL);
    }
}

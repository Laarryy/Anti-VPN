package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChainFactory;
import java.util.Map;
import java.util.Optional;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommandIssuer issuer;
    private final String ip;
    private final TaskChainFactory taskFactory;

    public TestCommand(CommandIssuer issuer, String ip, TaskChainFactory taskFactory) {
        this.issuer = issuer;
        this.ip = ip;
        this.taskFactory = taskFactory;
    }

    public void run() {
        issuer.sendInfo(Message.TEST__BEGIN, "{ip}", ip);

        chain
                .<Optional<Map<String, Optional<Boolean>>>>asyncCallback((v, f) -> {
                    try {
                        f.accept(Optional.of(api.testAllSources(ip)));
                        return;
                    } catch (APIException ex) {
                        if (ConfigUtil.getDebugOrFalse()) {
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

                    for (Map.Entry<String, Optional<Boolean>> kvp : f.get().entrySet()) {
                        if (!kvp.getValue().isPresent()) {
                            issuer.sendInfo(Message.TEST__ERROR, "{source}", kvp.getKey());
                            continue;
                        }
                        issuer.sendInfo(kvp.getValue().get() ? Message.TEST__VPN_DETECTED : Message.TEST__NO_VPN_DETECTED, "{source}", kvp.getKey());
                    }
                    issuer.sendInfo(Message.TEST__END, "{ip}", ip);
                })
                .execute();
    }
}

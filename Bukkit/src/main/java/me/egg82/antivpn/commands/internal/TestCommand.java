package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChainFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.lang.Message;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TestCommand extends AbstractCommand {
    private final String ip;

    public TestCommand(@NonNull CommandIssuer issuer, @NonNull TaskChainFactory taskFactory, @NonNull String ip) {
        super(issuer, taskFactory);
        this.ip = ip;
    }

    public void run() {
        issuer.sendInfo(Message.TEST__BEGIN, "{ip}", ip);

        SourceManager sourceManager = VPNAPIProvider.getInstance().getSourceManager();

        taskFactory.<Void>newChain()
                .<Map<String, Optional<Boolean>>>asyncCallback((v, r) -> {
                    Map<String, Optional<Boolean>> retVal = new HashMap<>();

                    List<Source<? extends SourceModel>> sources = sourceManager.getSources();
                    // TODO: multi-thread this
                    for (Source<? extends SourceModel> source : sources) {
                        try {
                            retVal.put(source.getName(), Optional.ofNullable(source.getResult(ip)
                                    .exceptionally(this::handleException)
                                    .join()));
                        } catch (Exception ignored) { }
                    }

                    r.accept(retVal);
                })
                .syncLast(v -> {
                    if (v.isEmpty()) {
                        issuer.sendError(Message.ERROR__INTERNAL);
                        return;
                    }

                    for (Map.Entry<String, Optional<Boolean>> kvp : v.entrySet()) {
                        if (!kvp.getValue().isPresent()) {
                            issuer.sendInfo(Message.TEST__ERROR, "{source}", kvp.getKey());
                            continue;
                        }
                        issuer.sendInfo(kvp.getValue().get() ? Message.TEST__VPN_DETECTED : Message.TEST__NO_VPN_DETECTED, "{source}", kvp.getKey());
                    }
                })
                .execute();
    }
}

package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.utils.ExceptionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;

public class TestCommand extends AbstractCommand {
    private final String ip;

    public TestCommand(@NotNull ProxyServer proxy, @NotNull CommandIssuer issuer, @NotNull String ip) {
        super(proxy, issuer);
        this.ip = ip;
    }

    @Override
    public void run() {
        issuer.sendInfo(MessageKey.TEST__BEGIN, "{ip}", ip);

        SourceManager sourceManager = VPNAPIProvider.getInstance().getSourceManager();

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        List<Source<? extends SourceModel>> sources = sourceManager.getSources();
        CountDownLatch latch = new CountDownLatch(sources.size());

        ConcurrentMap<String, Optional<Boolean>> results = new ConcurrentHashMap<>(sources.size());

        for (Source<? extends SourceModel> source : sources) {
            if (cachedConfig.getDebug()) {
                logger.info("Getting result from source " + source.getName() + ".");
            }

            futures.add(source.getResult(ip).whenCompleteAsync((val, ex) -> {
                if (ex != null) {
                    ExceptionUtil.handleException(ex, logger);
                    latch.countDown();
                    return;
                }
                results.put(source.getName(), Optional.ofNullable(val));
                latch.countDown();
            }));
        }

        try {
            if (!latch.await(20L, TimeUnit.SECONDS)) {
                logger.warn("Test timed out before all sources could be queried.");
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        for (CompletableFuture<Boolean> future : futures) {
            future.cancel(true); // Kill it with fire
        }

        Map<String, Optional<Boolean>> retVal = new LinkedHashMap<>(sources.size());
        for (Source<? extends SourceModel> source : sources) {
            retVal.put(source.getName(), results.computeIfAbsent(source.getName(), k -> Optional.empty()));
        }

        if (retVal.isEmpty()) {
            issuer.sendError(MessageKey.ERROR__INTERNAL);
            return;
        }

        for (Map.Entry<String, Optional<Boolean>> kvp : retVal.entrySet()) {
            if (!kvp.getValue().isPresent()) {
                issuer.sendInfo(MessageKey.TEST__ERROR, "{source}", kvp.getKey());
                continue;
            }
            issuer.sendInfo(kvp.getValue().get() ? MessageKey.TEST__VPN_DETECTED : MessageKey.TEST__NO_VPN_DETECTED, "{source}", kvp.getKey());
        }
        issuer.sendInfo(MessageKey.TEST__END, "{ip}", ip);
    }
}

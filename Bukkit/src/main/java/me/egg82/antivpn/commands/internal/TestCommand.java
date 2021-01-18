package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import java.util.*;
import java.util.concurrent.*;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.utils.ExceptionUtil;
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

        TaskChain<Void> chain = taskFactory.newChain();
        chain.setErrorHandler((ex, task) -> ExceptionUtil.handleException(ex, logger));
        chain
                .<Map<String, Optional<Boolean>>>asyncCallback((v, r) -> {
                    CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
                    if (cachedConfig == null) {
                        logger.error("Cached config could not be fetched.");
                        r.accept(new HashMap<>());
                        return;
                    }

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

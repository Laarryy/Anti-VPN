package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
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
                    CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
                    if (cachedConfig == null) {
                        logger.error("Cached config could not be fetched.");
                        r.accept(new HashMap<>());
                        return;
                    }

                    ExecutorService pool = Executors.newWorkStealingPool(cachedConfig.getThreads());
                    List<Source<? extends SourceModel>> sources = sourceManager.getSources();
                    CountDownLatch latch = new CountDownLatch(sources.size());

                    ConcurrentMap<String, Optional<Boolean>> results = new ConcurrentHashMap<>(sources.size());

                    for (Source<? extends SourceModel> source : sources) {
                        pool.submit(() -> {
                            if (cachedConfig.getDebug()) {
                                logger.info("Getting result from source " + source.getName() + ".");
                            }

                            try {
                                results.put(source.getName(), Optional.ofNullable(source.getResult(ip)
                                        .exceptionally(this::handleException)
                                        .join()));
                            } catch (CompletionException ignored) {
                            } catch (Exception ex) {
                                if (cachedConfig.getDebug()) {
                                    logger.error(ex.getMessage(), ex);
                                } else {
                                    logger.error(ex.getMessage());
                                }
                            }
                            latch.countDown();
                        });
                    }

                    try {
                        if (!latch.await(20L, TimeUnit.SECONDS)) {
                            logger.warn("Test timed out before all sources could be queried.");
                        }
                    } catch (InterruptedException ex) {
                        if (cachedConfig.getDebug()) {
                            logger.error(ex.getMessage(), ex);
                        } else {
                            logger.error(ex.getMessage());
                        }
                        Thread.currentThread().interrupt();
                    }
                    pool.shutdownNow(); // Kill it with fire

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

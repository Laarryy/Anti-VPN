package me.egg82.antivpn.commands.internal;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import me.egg82.antivpn.locale.BukkitLocalizedCommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

public class TestCommand extends AbstractCommand {
    public TestCommand(@NotNull PaperCommandManager<BukkitLocalizedCommandSender> commandManager) {
        super(commandManager);
    }

    @Override
    public void execute(@NonNull CommandContext<BukkitLocalizedCommandSender> commandContext) { }

    /*private final String ip;

    public TestCommand(@NotNull CommandIssuer issuer, @NotNull TaskChainFactory taskFactory, @NotNull String ip) {
        super(issuer, taskFactory);
        this.ip = ip;
    }

    public void run() {
        issuer.sendInfo(MessageKey.TEST__BEGIN, "{ip}", ip);

        SourceManager sourceManager = VPNAPIProvider.getInstance().getSourceManager();

        TaskChain<Void> chain = taskFactory.newChain();
        chain.setErrorHandler((ex, task) -> ExceptionUtil.handleException(ex, logger));
        chain
                .<Map<String, Optional<Boolean>>>asyncCallback((v, r) -> {
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

                    r.accept(retVal);
                })
                .syncLast(v -> {
                    if (v.isEmpty()) {
                        issuer.sendError(MessageKey.ERROR__INTERNAL);
                        return;
                    }

                    for (Map.Entry<String, Optional<Boolean>> kvp : v.entrySet()) {
                        if (!kvp.getValue().isPresent()) {
                            issuer.sendInfo(MessageKey.TEST__ERROR, "{source}", kvp.getKey());
                            continue;
                        }
                        issuer.sendInfo(kvp.getValue().get() ? MessageKey.TEST__VPN_DETECTED : MessageKey.TEST__NO_VPN_DETECTED, "{source}", kvp.getKey());
                    }
                    issuer.sendInfo(MessageKey.TEST__END, "{ip}", ip);
                })
                .execute();
    }*/
}

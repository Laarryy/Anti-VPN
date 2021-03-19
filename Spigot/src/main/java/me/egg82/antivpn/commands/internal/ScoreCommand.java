package me.egg82.antivpn.commands.internal;

import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.context.CommandContext;
import me.egg82.antivpn.locale.BukkitLocalizedCommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

public class ScoreCommand extends AbstractCommand {
    public ScoreCommand(@NotNull BukkitCommandManager<BukkitLocalizedCommandSender> commandManager) {
        super(commandManager);
    }

    @Override
    public void execute(@NonNull CommandContext<BukkitLocalizedCommandSender> commandContext) { }

    /*private final String sourceName;

    private static final DecimalFormat format = new DecimalFormat("##0.00");

    public ScoreCommand(@NotNull CommandIssuer issuer, @NotNull TaskChainFactory taskFactory, @NotNull String sourceName) {
        super(issuer, taskFactory);
        this.sourceName = sourceName;
    }

    public void run() {
        issuer.sendInfo(MessageKey.SCORE__BEGIN, "{source}", sourceName);

        SourceManager sourceManager = VPNAPIProvider.getInstance().getSourceManager();

        TaskChain<Void> chain = taskFactory.newChain();
        chain.setErrorHandler((ex, task) -> ExceptionUtil.handleException(ex, logger));
        chain
                .<Source<? extends SourceModel>>currentCallback((v, r) -> r.accept(sourceManager.getSource(sourceName)))
                .abortIfNull(this.handleAbort)
                .sync(v -> {
                    issuer.sendInfo(MessageKey.SCORE__TYPE, "{type}", "NordVPN");
                    return v;
                })
                .async(v -> {
                    test(v, "NordVPN", DNSUtil.getNordVpnIps());
                    return v;
                })
                .sync(v -> {
                    issuer.sendInfo(MessageKey.SCORE__SLEEP);
                    return v;
                })
                .delay(60, TimeUnit.SECONDS)
                .sync(v -> {
                    issuer.sendInfo(MessageKey.SCORE__TYPE, "{type}", "Cryptostorm");
                    return v;
                })
                .async(v -> {
                    test(v, "Cryptostorm", DNSUtil.getCryptostormIps());
                    return v;
                })
                .sync(v -> {
                    issuer.sendInfo(MessageKey.SCORE__SLEEP);
                    return v;
                })
                .delay(60, TimeUnit.SECONDS)
                .sync(v -> {
                    issuer.sendInfo(MessageKey.SCORE__TYPE, "{type}", "random home IPs");
                    return v;
                })
                .async(v -> {
                    test(v, "Random home IP", DNSUtil.getHomeIps(), true);
                    return v;
                })
                .syncLast(v -> issuer.sendInfo(MessageKey.SCORE__END, "{source}", v.getName()))
                .execute();
    }

    private void test(@NotNull Source<? extends SourceModel> source, @NotNull String vpnName, @NotNull Set<String> ips) {
        test(source, vpnName, ips, false);
    }

    private void test(@NotNull Source<? extends SourceModel> source, @NotNull String vpnName, @NotNull Set<String> ips, boolean flipResult) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Testing against " + vpnName);
        }

        double error = 0.0d;
        double good = 0.0d;

        int i = 0;
        for (String ip : ips) {
            i++;
            try {
                if (source.getName().equalsIgnoreCase("getipintel")) {
                    Thread.sleep(5000L); // 15req/min max, so every 4 seconds. 5 to be safe.
                } else {
                    Thread.sleep(1000L);
                }
            } catch (IllegalArgumentException ex) {
                logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            } catch (InterruptedException ex) {
                logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                Thread.currentThread().interrupt();
            }

            if (ConfigUtil.getDebugOrFalse()) {
                logger.info("Testing " + ip + " (" + i + "/" + ips.size() + ")");
            }

            boolean result;
            try {
                Boolean val = source.getResult(ip).get();
                if (val == null) {
                    error += 1;
                    continue;
                }
                result = val;
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                error += 1;
                continue;
            } catch (ExecutionException | CancellationException ex) {
                ExceptionUtil.handleException(ex, logger);
                if (!(ex.getCause() instanceof APIException) || !((APIException) ex.getCause()).isHard()) {
                    error += 1;
                }
                continue;
            }

            if ((!flipResult && result) || (flipResult && !result)) {
                good++;
            }
        }

        if (error > 0) {
            issuer.sendInfo(MessageKey.SCORE__ERROR, "{source}", source.getName(), "{type}", vpnName, "{percent}", format.format((error / ips.size()) * 100.0d));
        }
        issuer.sendInfo(MessageKey.SCORE__SCORE, "{source}", source.getName(), "{type}", vpnName, "{percent}", format.format((good / ips.size()) * 100.0d));
    }*/
}

package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChainFactory;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.utils.DNSUtil;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ScoreCommand extends AbstractCommand {
    private final String sourceName;

    private static final DecimalFormat format = new DecimalFormat("##0.00");

    public ScoreCommand(@NonNull CommandIssuer issuer, @NonNull TaskChainFactory taskFactory, @NonNull String sourceName) {
        super(issuer, taskFactory);
        this.sourceName = sourceName;
    }

    public void run() {
        issuer.sendInfo(Message.SCORE__BEGIN, "{source}", sourceName);

        SourceManager sourceManager = VPNAPIProvider.getInstance().getSourceManager();

        taskFactory.<Void>newChain()
                .<Source<? extends SourceModel>>currentCallback((v, r) -> r.accept(sourceManager.getSource(sourceName)))
                .abortIfNull(this.handleAbort)
                .sync(v -> {
                    issuer.sendInfo(Message.SCORE__TYPE, "{type}", "NordVPN");
                    return v;
                })
                .async(v -> {
                    test(v, "NordVPN", DNSUtil.getNordVpnIps());
                    return v;
                })
                .sync(v -> {
                    issuer.sendInfo(Message.SCORE__SLEEP);
                    return v;
                })
                .delay(60, TimeUnit.SECONDS)
                .sync(v -> {
                    issuer.sendInfo(Message.SCORE__TYPE, "{type}", "Cryptostorm");
                    return v;
                })
                .async(v -> {
                    test(v, "Cryptostorm", DNSUtil.getCryptostormIps());
                    return v;
                })
                .sync(v -> {
                    issuer.sendInfo(Message.SCORE__SLEEP);
                    return v;
                })
                .delay(60, TimeUnit.SECONDS)
                .sync(v -> {
                    issuer.sendInfo(Message.SCORE__TYPE, "{type}", "random home IPs");
                    return v;
                })
                .async(v -> {
                    test(v, "Random home IP", DNSUtil.getHomeIps(), true);
                    return v;
                })
                .syncLast(v -> issuer.sendInfo(Message.SCORE__END, "{source}", v.getName()))
                .execute();
    }

    private void test(@NonNull Source<? extends SourceModel> source, @NonNull String vpnName, @NonNull Set<String> ips) {
        test(source, vpnName, ips, false);
    }

    private void test(@NonNull Source<? extends SourceModel> source, @NonNull String vpnName, @NonNull Set<String> ips, boolean flipResult) {
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
                logger.error(ex.getMessage(), ex);
            } catch (InterruptedException ex) {
                logger.error(ex.getMessage(), ex);
                Thread.currentThread().interrupt();
            }

            if (ConfigUtil.getDebugOrFalse()) {
                logger.info("Testing " + ip + " (" + i + "/" + ips.size() + ")");
            }

            Boolean result;
            try {
                result = source.getResult(ip)
                        .exceptionally(this::handleException)
                        .join();
                if (result == null) {
                    error += 1;
                    continue;
                }
            } catch (Exception ex) {
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
            issuer.sendInfo(Message.SCORE__ERROR, "{source}", source.getName(), "{type}", vpnName, "{percent}", format.format((error / ips.size()) * 100.0d));
        }
        issuer.sendInfo(Message.SCORE__SCORE, "{source}", source.getName(), "{type}", vpnName, "{percent}", format.format((good / ips.size()) * 100.0d));
    }
}

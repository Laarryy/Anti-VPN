package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChainFactory;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.utils.DNSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScoreCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ScoreCommand.class);

    private final CommandIssuer issuer;
    private final String source;
    private final TaskChainFactory taskFactory;

    private static final DecimalFormat format = new DecimalFormat("##0.00");

    public ScoreCommand(CommandIssuer issuer, String source, TaskChainFactory taskFactory) {
        this.issuer = issuer;
        this.source = source;
        this.taskFactory = taskFactory;
    }

    public void run() {
        issuer.sendInfo(Message.SCORE__BEGIN, "{source}", source);

        taskFactory.<Void>newChain()
                .sync(() -> issuer.sendInfo(Message.SCORE__TYPE, "{type}", "NordVPN"))
                .async(() -> test(issuer, source, "NordVPN", DNSUtil.getNordVpnIps()))
                .sync(() -> issuer.sendInfo(Message.SCORE__SLEEP))
                .delay(60, TimeUnit.SECONDS)
                .sync(() -> issuer.sendInfo(Message.SCORE__TYPE, "{type}", "Cryptostorm"))
                .async(() -> test(issuer, source, "Cryptostorm", DNSUtil.getCryptostormIps()))
                .sync(() -> issuer.sendInfo(Message.SCORE__SLEEP))
                .delay(60, TimeUnit.SECONDS)
                .sync(() -> issuer.sendInfo(Message.SCORE__TYPE, "{type}", "random home IPs"))
                .async(() -> test(issuer, source, "Random home IP", DNSUtil.getHomeIps(), true))
                .sync(() -> issuer.sendInfo(Message.SCORE__END, "{source}", source))
                .execute();
    }

    private void test(CommandIssuer issuer, String source, String vpnName, Set<String> ips) {
        test(issuer, source, vpnName, ips, false);
    }

    private void test(CommandIssuer issuer, String source, String vpnName, Set<String> ips, boolean flipResult) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Testing against " + vpnName);
        }

        double error = 0.0d;
        double good = 0.0d;

        int i = 0;
        for (String ip : ips) {
            i++;
            try {
                if (source.equalsIgnoreCase("getipintel")) {
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

            boolean result;
            try {
                result = api.getSourceResult(ip, source);
            } catch (APIException ex) {
                if (ex.isHard()) {
                    logger.error(ex.getMessage(), ex);
                    continue;
                }
                error++;
                continue;
            }

            if ((!flipResult && result) || (flipResult && !result)) {
                good++;
            }
        }

        if (error > 0) {
            issuer.sendInfo(Message.SCORE__ERROR, "{source}", source, "{type}", vpnName, "{percent}", format.format((error / ips.size()) * 100.0d));
        }
        issuer.sendInfo(Message.SCORE__SCORE, "{source}", source, "{type}", vpnName, "{percent}", format.format((good / ips.size()) * 100.0d));
    }
}

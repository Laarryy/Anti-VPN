package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChain;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.utils.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScoreCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ScoreCommand.class);

    private final CommandIssuer issuer;
    private final String source;
    private final TaskChain<?> chain;

    private final VPNAPI api = VPNAPI.getInstance();

    private static final DecimalFormat format = new DecimalFormat("##0.00");

    public ScoreCommand(CommandIssuer issuer, String source, TaskChain<?> chain) {
        this.issuer = issuer;
        this.source = source;
        this.chain = chain;
    }

    public void run() {
        issuer.sendInfo(Message.SCORE__BEGIN, "{source}", source);

        chain
                .sync(() -> issuer.sendInfo(Message.SCORE__TYPE, "{type}", "NordVPN"))
                .async(() -> test(issuer, source, "NordVPN", getNordVPNIPs()))
                .sync(() -> issuer.sendInfo(Message.SCORE__SLEEP))
                .delay(60, TimeUnit.SECONDS)
                .sync(() -> issuer.sendInfo(Message.SCORE__TYPE, "{type}", "Cryptostorm"))
                .async(() -> test(issuer, source, "Cryptostorm", getCryptostormIPs()))
                .sync(() -> issuer.sendInfo(Message.SCORE__SLEEP))
                .delay(60, TimeUnit.SECONDS)
                .sync(() -> issuer.sendInfo(Message.SCORE__TYPE, "{type}", "random home IPs"))
                .async(() -> test(issuer, source, "Random home IP", getHomeIPs(), true))
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

    private Set<String> getNordVPNIPs() {
        Set<String> dns = new HashSet<>();
        dns.addAll(validNordVPN.get("al{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("ar{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("au{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("at{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("be{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("ba{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("br{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("bg{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("ca{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("cl{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("cr{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("hr{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("cy{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("cz{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("dk{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("ee{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("fi{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("fr{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("ge{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("de{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("gr{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("hk{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("hu{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("is{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("in{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("id{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("il{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("it{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("jp{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("lv{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("lu{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("my{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("mx{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("md{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("nl{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("nz{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("mk{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("no{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("pl{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("pt{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("ro{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("rs{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("sg{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("sk{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("si{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("za{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("kr{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("es{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("se{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("ch{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("tw{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("th{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("tr{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("ua{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("uk{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("us{}.nordvpn.com"));
        dns.addAll(validNordVPN.get("vn{}.nordvpn.com"));
        return getIPs(dns.toArray(new String[0]), 50);
    }

    private static LoadingCache<String, Set<String>> validNordVPN = Caffeine.newBuilder().build(ScoreCommand::findNordVPN);

    private static Set<String> findNordVPN(String dns) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Building NordVPN set " + dns.replace("{}", ""));
        }

        Set<String> retVal = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            try {
                String name = dns.replace("{}", String.valueOf(i));
                InetAddress.getByName(name);
                retVal.add(name);
            } catch (UnknownHostException ignored) { }
        }
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Got " + retVal.size() + " value(s) for NordVPN set " + dns.replace("{}", ""));
        }
        return retVal;
    }

    private Set<String> getCryptostormIPs() {
        String[] dns = new String[] {
                "balancer.cstorm.is",
                "balancer.cstorm.net",
                "balancer.cryptostorm.ch",
                "balancer.cryptostorm.pw"
        };
        return getIPs(dns, 50);
    }

    private static LoadingCache<String, Set<String>> records = Caffeine.newBuilder().build(ScoreCommand::collectRecords);

    private static Set<String> collectRecords(String dns) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Collecting A records for " + dns);
        }
        Set<String> retVal = new HashSet<>();
        try {
            InitialDirContext context = new InitialDirContext();
            Attributes attributes = context.getAttributes("dns:/" + dns, new String[] { "A" });
            NamingEnumeration<?> attributeEnum = attributes.get("A").getAll();
            while (attributeEnum.hasMore()) {
                retVal.add(attributeEnum.next().toString());
            }
        } catch (NamingException ex) {
            logger.error(ex.getMessage(), ex);
        }
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Got " + retVal.size() + " record(s) for " + dns);
        }
        return retVal;
    }

    private Set<String> getHomeIPs() {
        String[] dns = new String[] {
                // Comcast - https://postmaster.comcast.net/dynamic-IP-ranges.html
                "24.0.0.0/12",
                "24.16.0.0/13",
                "24.30.0.0/17",
                "24.34.0.0/16",
                "24.60.0.0/14",
                "24.91.0.0/16",
                "24.98.0.0/15",
                "24.118.0.0/16",
                "24.125.0.0/16",
                "24.126.0.0/15",
                "24.128.0.0/16",
                "24.129.0.0/17",
                "24.130.0.0/15",
                "24.147.0.0/16",
                "24.218.0.0/16",
                "24.245.0.0/18",
                "50.128.0.0/10",
                "65.34.128.0/17",
                "65.96.0.0/16",
                "66.30.0.0/15",
                "66.41.0.0/16",
                "66.56.0.0/18",
                "66.176.0.0/15",
                "66.229.0.0/16",
                "67.160.0.0/12",
                "67.176.0.0/15",
                "67.180.0.0/14",
                "67.184.0.0/13",
                "68.32.0.0/11",
                "68.80.0.0/14",
                "68.84.0.0/16",
                "69.136.0.0/15",
                "69.138.0.0/16",
                "69.139.0.0/17",
                "69.140.0.0/14",
                "69.180.0.0/15",
                "69.242.0.0/15",
                "69.244.0.0/14",
                "69.248.0.0/14",
                "69.253.0.0/16",
                "69.254.0.0/15",
                "71.56.0.0/13",
                "71.192.0.0/12",
                "71.224.0.0/12",
                "73.0.0.0/8",
                "75.64.0.0/13",
                "75.72.0.0/15",
                "75.74.0.0/16",
                "75.75.0.0/17",
                "75.75.128.0/18",
                "76.16.0.0/12",
                "76.97.0.0/16",
                "76.98.0.0/15",
                "76.100.0.0/14",
                "76.104.0.0/13",
                "76.112.0.0/12",
                "98.192.0.0/13",
                "98.200.0.0/14",
                "98.204.0.0/16",
                "98.206.0.0/15",
                "98.208.0.0/12",
                "98.224.0.0/12",
                "98.240.0.0/16",
                "98.242.0.0/15",
                "98.244.0.0/14",
                "98.248.0.0/13",
                "107.2.0.0/15",
                "107.4.0.0/15",
                "174.48.0.0/12",
                "2001:558:6000::/36"
        };
        return getIPs(dns, 50);
    }

    private Set<String> getIPs(String[] dns, int count) {
        Set<String> retVal = new HashSet<>();

        int fails = 0;
        while (retVal.size() < count && fails < 1000) {
            String name;
            do {
                name = dns[(int) fairRoundedRandom(0L, (long) dns.length - 1L)];
            } while (name == null);

            if (ValidationUtil.isValidIp(name)) {
                if (!retVal.add(name)) {
                    fails++;
                }
            } else if (ValidationUtil.isValidIPRange(name)) {
                if (!retVal.addAll(getIPs(name, 1))) {
                    fails++;
                }
            } else {
                List<String> r = new ArrayList<>(records.get(name));
                if (r.isEmpty()) {
                    continue;
                }
                if (!retVal.add(r.get((int) fairRoundedRandom(0L, (long) r.size() - 1L)))) {
                    fails++;
                }
            }
        }

        return retVal;
    }

    private Set<String> getIPs(String mask, int count) {
        Set<String> retVal = new HashSet<>();
        IPAddress range = new IPAddressString(mask).getAddress();

        int fails = 0;
        while (retVal.size() < count && fails < 1000) {
            long getIndex = fairRoundedRandom(0L, range.getCount().longValue());
            long i = 0;
            for (IPAddress ip : range.getIterable()) {
                if (i == getIndex) {
                    String str = ip.toCanonicalString();
                    int idx = str.indexOf('/');
                    if (idx > -1) {
                        str = str.substring(0, idx);
                    }
                    if (!retVal.add(str)) {
                        fails++;
                    }
                    if (retVal.size() >= count || fails >= 1000) {
                        break;
                    }
                    getIndex = fairRoundedRandom(0L, range.getCount().longValue());
                    if (getIndex <= i) {
                        break;
                    }
                }
                i++;
            }
        }

        return retVal;
    }

    private long fairRoundedRandom(long min, long max) {
        long num;
        max++;

        do {
            num = (long) Math.floor(Math.random() * (max - min) + min);
        } while (num > max - 1);

        return num;
    }
}

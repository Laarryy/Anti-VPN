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
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.enums.Message;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScoreCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommandIssuer issuer;
    private final String source;
    private final TaskChain<?> chain;

    private final VPNAPI api = VPNAPI.getInstance();

    private static final DecimalFormat format = new DecimalFormat(".##");

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
        double error = 0.0d;
        double good = 0.0d;

        for (String ip : ips) {
            try {
                Thread.sleep(5000L);
            } catch (IllegalArgumentException ex) {
                logger.error(ex.getMessage(), ex);
            } catch (InterruptedException ex) {
                logger.error(ex.getMessage(), ex);
                Thread.currentThread().interrupt();
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
        issuer.sendInfo(Message.SCORE__SCORE, "{source}", vpnName, "{type}", vpnName, "{percent}", format.format((error / ips.size()) * 100.0d));
    }

    private Set<String> getNordVPNIPs() {
        String[] dns = new String[] {
                replaceNordVPN("al{}.nordvpn.com"),
                replaceNordVPN("ar{}.nordvpn.com"),
                replaceNordVPN("au{}.nordvpn.com"),
                replaceNordVPN("at{}.nordvpn.com"),
                replaceNordVPN("be{}.nordvpn.com"),
                replaceNordVPN("ba{}.nordvpn.com"),
                replaceNordVPN("br{}.nordvpn.com"),
                replaceNordVPN("bg{}.nordvpn.com"),
                replaceNordVPN("ca{}.nordvpn.com"),
                replaceNordVPN("cl{}.nordvpn.com"),
                replaceNordVPN("cr{}.nordvpn.com"),
                replaceNordVPN("hr{}.nordvpn.com"),
                replaceNordVPN("cy{}.nordvpn.com"),
                replaceNordVPN("cz{}.nordvpn.com"),
                replaceNordVPN("dk{}.nordvpn.com"),
                replaceNordVPN("ee{}.nordvpn.com"),
                replaceNordVPN("fi{}.nordvpn.com"),
                replaceNordVPN("fr{}.nordvpn.com"),
                replaceNordVPN("ge{}.nordvpn.com"),
                replaceNordVPN("de{}.nordvpn.com"),
                replaceNordVPN("gr{}.nordvpn.com"),
                replaceNordVPN("hk{}.nordvpn.com"),
                replaceNordVPN("hu{}.nordvpn.com"),
                replaceNordVPN("is{}.nordvpn.com"),
                replaceNordVPN("in{}.nordvpn.com"),
                replaceNordVPN("id{}.nordvpn.com"),
                replaceNordVPN("il{}.nordvpn.com"),
                replaceNordVPN("it{}.nordvpn.com"),
                replaceNordVPN("jp{}.nordvpn.com"),
                replaceNordVPN("lv{}.nordvpn.com"),
                replaceNordVPN("lu{}.nordvpn.com"),
                replaceNordVPN("my{}.nordvpn.com"),
                replaceNordVPN("mx{}.nordvpn.com"),
                replaceNordVPN("md{}.nordvpn.com"),
                replaceNordVPN("nl{}.nordvpn.com"),
                replaceNordVPN("nz{}.nordvpn.com"),
                replaceNordVPN("mk{}.nordvpn.com"),
                replaceNordVPN("no{}.nordvpn.com"),
                replaceNordVPN("pl{}.nordvpn.com"),
                replaceNordVPN("pt{}.nordvpn.com"),
                replaceNordVPN("ro{}.nordvpn.com"),
                replaceNordVPN("rs{}.nordvpn.com"),
                replaceNordVPN("sg{}.nordvpn.com"),
                replaceNordVPN("sk{}.nordvpn.com"),
                replaceNordVPN("si{}.nordvpn.com"),
                replaceNordVPN("za{}.nordvpn.com"),
                replaceNordVPN("kr{}.nordvpn.com"),
                replaceNordVPN("es{}.nordvpn.com"),
                replaceNordVPN("se{}.nordvpn.com"),
                replaceNordVPN("ch{}.nordvpn.com"),
                replaceNordVPN("tw{}.nordvpn.com"),
                replaceNordVPN("th{}.nordvpn.com"),
                replaceNordVPN("tr{}.nordvpn.com"),
                replaceNordVPN("ua{}.nordvpn.com"),
                replaceNordVPN("uk{}.nordvpn.com"),
                replaceNordVPN("us{}.nordvpn.com"),
                replaceNordVPN("vn{}.nordvpn.com")
        };
        return getIPs(dns, 50);
    }

    private LoadingCache<String, Set<String>> validNordVPN = Caffeine.newBuilder().build(this::findNordVPN);

    private String replaceNordVPN(String dns) {
        List<String> ips = new ArrayList<>(validNordVPN.get(dns));
        if (ips.isEmpty()) {
            return null;
        }
        return ips.get((int) fairRoundedRandom(0L, (long) ips.size() - 1L));
    }

    private Set<String> findNordVPN(String dns) {
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
        return getIPs(dns, 25);
    }

    private LoadingCache<String, Set<String>> records = Caffeine.newBuilder().build(this::collectRecords);

    private Set<String> collectRecords(String dns) {
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
                "2001:558:6000::/36",
                // Centurylink - https://www.ctl.io/knowledge-base/network/centurylink-cloud-public-ip-listing/
                "65.127.194.144/29",
                "65.151.184.0/22",
                "64.69.71.128/25",
                "65.39.180.0/24",
                "65.39.184.0/25",
                "65.151.128.0/22",
                "216.187.73.144/28",
                "216.187.110.0/24",
                "65.151.132.0/23",
                "66.155.96.0/24",
                "66.155.100.0/24",
                "69.28.224.128/25",
                "70.33.208.0/25",
                "70.33.239.96/28",
                "70.33.239.128/25",
                "107.6.43.0/24",
                "206.152.32.0/21",
                "206.152.45.0/24",
                "66.151.140.0/23",
                "66.155.4.0/24",
                "66.155.94.0/24",
                "65.151.172.0/22",
                "66.155.18.0/23",
                "66.155.27.0/24",
                "66.155.28.0/24",
                "176.74.168.0/25",
                "176.74.179.0/25",
                "206.142.240.0/21",
                "207.82.88.0/24",
                "64.74.98.0/24",
                "64.74.229.0/24",
                "64.94.35.32/28",
                "66.150.98.0/23",
                "66.150.105.0/24",
                "69.25.149.0/24",
                "72.5.194.0/24",
                "72.5.203.0/24",
                "74.201.4.0/24",
                "74.217.15.0/24",
                "74.201.135.0/24",
                "74.201.140.0/24",
                "74.201.165.0/24",
                "74.201.226.0/24",
                "74.201.232.0/24",
                "74.201.237.0/24",
                "74.201.240.0/24",
                "205.139.16.0/22",
                "205.139.24.0/25",
                "64.15.176.0/24",
                "64.15.180.16/28",
                "64.15.182.0/24",
                "64.15.183.0/24",
                "64.15.184.0/21",
                "64.211.224.0/25",
                "206.128.134.0/23",
                "206.128.136.0/23",
                "206.128.152.0/21",
                "206.128.173.0/24",
                "206.128.176.0/23",
                "65.151.188.0/22",
                "64.94.142.8/29",
                "64.94.114.0/24",
                "64.94.138.0/24",
                "66.150.160.0/25",
                "66.150.174.0/24",
                "69.25.131.0/24",
                "70.42.161.0/24",
                "70.42.168.0/24"
        };
        return getIPs(dns, 50);
    }

    private Set<String> getIPs(String[] dns, int count) {
        Set<String> retVal = new HashSet<>();

        while (retVal.size() < count) {
            String name = dns[(int) fairRoundedRandom(0L, (long) dns.length - 1L)];
            if (ValidationUtil.isValidIp(name)) {
                retVal.add(name);
            } else if (ValidationUtil.isValidIPRange(name)) {
                retVal.addAll(getIPs(name, 1));
            } else {
                List<String> r = new ArrayList<>(records.get(name));
                if (r.isEmpty()) {
                    continue;
                }
                retVal.add(r.get((int) fairRoundedRandom(0L, (long) r.size() - 1L)));
            }
        }

        return retVal;
    }

    private Set<String> getIPs(String mask, int count) {
        Set<String> retVal = new HashSet<>();
        IPAddress range = new IPAddressString(mask).getAddress();

        while (retVal.size() < count) {
            long getIndex = fairRoundedRandom(0L, range.getCount().longValue());
            long i = 0;
            for (IPAddress ip : range.getIterable()) {
                if (i == getIndex) {
                    retVal.add(ip.toCanonicalString());
                    if (retVal.size() >= count) {
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

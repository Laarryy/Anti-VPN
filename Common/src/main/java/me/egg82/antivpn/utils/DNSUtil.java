package me.egg82.antivpn.utils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.logging.GELFLogger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DNSUtil {
    private static final Logger logger = LoggerFactory.getLogger(DNSUtil.class);

    private DNSUtil() { }

    public static @NotNull Set<@NotNull String> getNordVpnIps() {
        Set<String> dns = new HashSet<>();
        dns.addAll(validNordVpn.get("al{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("ar{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("au{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("at{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("be{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("ba{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("br{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("bg{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("ca{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("cl{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("cr{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("hr{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("cy{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("cz{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("dk{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("ee{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("fi{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("fr{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("ge{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("de{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("gr{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("hk{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("hu{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("is{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("in{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("id{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("il{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("it{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("jp{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("lv{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("lu{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("my{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("mx{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("md{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("nl{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("nz{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("mk{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("no{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("pl{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("pt{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("ro{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("rs{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("sg{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("sk{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("si{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("za{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("kr{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("es{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("se{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("ch{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("tw{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("th{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("tr{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("ua{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("uk{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("us{}.nordvpn.com"));
        dns.addAll(validNordVpn.get("vn{}.nordvpn.com"));
        return getIps(dns.toArray(new String[0]), 50);
    }

    private static final LoadingCache<String, Set<String>> validNordVpn = Caffeine.newBuilder().build(DNSUtil::findNordVpn);

    private static @NotNull Set<@NotNull String> findNordVpn(@NotNull String dns) {
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

    public static @NotNull Set<@NotNull String> getCryptostormIps() {
        String[] dns = new String[] {
                "balancer.cstorm.is",
                "balancer.cstorm.net",
                "balancer.cryptostorm.ch",
                "balancer.cryptostorm.pw"
        };
        return getIps(dns, 50);
    }

    private static final LoadingCache<String, Set<String>> records = Caffeine.newBuilder().build(DNSUtil::collectRecords);

    private static @NotNull Set<@NotNull String> collectRecords(@NotNull String dns) {
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
            GELFLogger.exception(logger, ex);
        }
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Got " + retVal.size() + " record(s) for " + dns);
        }
        return retVal;
    }

    public static @NotNull Set<@NotNull String> getHomeIps() {
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
        return getIps(dns, 50);
    }

    private static @NotNull Set<@NotNull String> getIps(@NotNull String @NotNull [] dns, int count) {
        Set<String> retVal = new HashSet<>();

        int fails = 0;
        while (retVal.size() < count && fails < 1000) {
            String name = dns[(int) fairRoundedRandom(0L, (long) dns.length - 1L)];

            if (ValidationUtil.isValidIp(name)) {
                if (!retVal.add(name)) {
                    fails++;
                }
            } else if (ValidationUtil.isValidIpRange(name)) {
                if (!retVal.addAll(getIps(name, 1))) {
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

    private static @NotNull Set<@NotNull String> getIps(@NotNull String mask, int count) {
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

    private static long fairRoundedRandom(long min, long max) {
        long num;
        max++;

        do {
            num = (long) Math.floor(Math.random() * (max - min) + min);
        } while (num > max - 1);

        return num;
    }
}

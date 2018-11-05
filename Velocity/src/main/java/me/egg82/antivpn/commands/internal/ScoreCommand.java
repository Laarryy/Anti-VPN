package me.egg82.antivpn.commands.internal;

import com.velocitypowered.api.command.CommandSource;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScoreCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommandSource source;
    private final String sourceName;

    private final VPNAPI api = VPNAPI.getInstance();

    private static final DecimalFormat format = new DecimalFormat(".##");

    public ScoreCommand(CommandSource source, String sourceName) {
        this.source = source;
        this.sourceName = sourceName;
    }

    public void run() {
        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Scoring ").color(TextColor.YELLOW)).append(TextComponent.of(sourceName).color(TextColor.WHITE)).append(TextComponent.of(", please wait..").color(TextColor.YELLOW)).build());

        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Scoring against ").color(TextColor.YELLOW)).append(TextComponent.of("NordVPN").color(TextColor.WHITE)).append(TextComponent.of("..").color(TextColor.YELLOW)).build());
        test(source, sourceName, "NordVPN", getNordVPNIPs());
        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Sleeping for one minute..").color(TextColor.YELLOW)).build());

        try {
            Thread.sleep(60000L);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        }

        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Scoring against ").color(TextColor.YELLOW)).append(TextComponent.of("Cryptostorm").color(TextColor.WHITE)).append(TextComponent.of("..").color(TextColor.YELLOW)).build());
        test(source, sourceName, "Cryptostorm", getCryptostormIPs());
        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Sleeping for one minute..").color(TextColor.YELLOW)).build());

        try {
            Thread.sleep(60000L);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        }

        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Scoring against ").color(TextColor.YELLOW)).append(TextComponent.of("random home IPs").color(TextColor.WHITE)).append(TextComponent.of("..").color(TextColor.YELLOW)).build());
        test(source, sourceName, "Random home IP", getHomeIPs(), true);
        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Score for ").color(TextColor.GREEN)).append(TextComponent.of(sourceName).color(TextColor.YELLOW)).append(TextComponent.of(" complete!").color(TextColor.GREEN)).build());
    }

    private void test(CommandSource source, String sourceName, String vpnName, Set<String> ips) {
        test(source, sourceName, vpnName, ips, false);
    }

    private void test(CommandSource source, String sourceName, String vpnName, Set<String> ips, boolean flipResult) {
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

            Optional<Boolean> result = api.getSourceResult(ip, sourceName);
            Boolean bool = result.orElse(null);
            if (bool == null) {
                error++;
                continue;
            }

            if ((!flipResult && bool) || (flipResult && !bool)) {
                good++;
            }
        }

        if (error > 0) {
            source.sendMessage(LogUtil.getHeading().append(LogUtil.getSourceHeading(sourceName)).append(TextComponent.of("Error ").color(TextColor.DARK_RED)).append(TextComponent.of(format.format((error / ips.size()) * 100.0d) + "%").color(TextColor.WHITE)).build());
        }
        source.sendMessage(LogUtil.getHeading().append(LogUtil.getSourceHeading(sourceName)).append(TextComponent.of(vpnName + " score: ").color(TextColor.YELLOW)).append(TextComponent.of(format.format((good / (ips.size() - error)) * 100.0d) + "%").color(TextColor.WHITE)).build());
    }

    private Set<String> getNordVPNIPs() {
        String[] dns = new String[] { "al9.nordvpn.com", "au154.nordvpn.com", "ca419.nordvpn.com", "ca398.nordvpn.com", "cz23.nordvpn.com", "fr33.nordvpn.com", "de114.nordvpn.com", "hk45.nordvpn.com",
                "id2.nordvpn.com", "jp15.nordvpn.com", "mx6.nordvpn.com", "nz20.nordvpn.com", "pl43.nordvpn.com", "ru13.nordvpn.com", "sg37.nordvpn.com", "kr13.nordvpn.com", "se140.nordvpn.com",
                "ua3.nordvpn.com", "uk357.nordvpn.com", "us2580.nordvpn.com", "vn4.nordvpn.com" };
        return getIPs(dns);
    }

    private Set<String> getCryptostormIPs() {
        String[] dns = new String[] { "windows-usnorth.cstorm.pw", "windows-useast.cstorm.pw", "windows-ussouth.cstorm.pw", "windows-uswest.cstorm.pw", "windows-canadawest.cstorm.pw",
                "windows-canadaeast.cstorm.pw", "windows-paris.cstorm.pw", "windows-rome.cstorm.pw", "windows-denmark.cstorm.pw", "windows-latvia.cstorm.pw", "windows-poland.cstorm.pw",
                "windows-finland.cstorm.pw", "windows-lisbon.cstorm.pw", "windows-england.cstorm.pw", "windows-dusseldorf.cstorm.pw", "windows-frankfurt.cstorm.pw", "windows-romania.cstorm.pw",
                "windows-netherlands.cstorm.pw", "windows-switzerland.cstorm.pw", "windows-sweden.cstorm.pw" };
        return getIPs(dns);
    }

    private Set<String> getHomeIPs() {
        Set<String> retVal = new HashSet<>();

        // Comcast
        retVal.addAll(getIPs("24.60.0.0/14", 2));
        retVal.addAll(getIPs("65.96.0.0/16", 2));
        retVal.addAll(getIPs("68.32.0.0/11", 2));
        retVal.addAll(getIPs("71.56.0.0/13", 2));
        retVal.addAll(getIPs("98.192.0.0/13", 2));
        // Centurylink
        retVal.addAll(getIPs("65.127.194.144/29", 2));
        retVal.addAll(getIPs("70.33.208.0/25", 2));
        retVal.addAll(getIPs("66.155.18.0/23", 2));
        retVal.addAll(getIPs("64.74.98.0/24", 2));
        retVal.addAll(getIPs("74.201.226.0/24", 2));

        return retVal;
    }

    private Set<String> getIPs(String[] dns) {
        Set<String> retVal = new HashSet<>();

        for (String name : dns) {
            try {
                InetAddress address = InetAddress.getByName(name);
                String ip = address.getHostAddress();
                if (ValidationUtil.isValidIp(ip)) {
                    retVal.add(ip);
                } else {
                    logger.warn(name + " does not have a valid IP: " + ip);
                }
            } catch (Exception ex) {
                logger.warn(name + " could not be fetched.");
            }
        }

        return retVal;
    }

    private Set<String> getIPs(String mask, int count) {
        SubnetUtils subnet = new SubnetUtils(mask);
        String[] addresses = subnet.getInfo().getAllAddresses();

        Set<String> retVal = new HashSet<>();
        while (retVal.size() < count) {
            retVal.add(addresses[fairRoundedRandom(0, addresses.length - 1)]);
        }

        return retVal;
    }

    private int fairRoundedRandom(int min, int max) {
        int num;
        max++;

        do {
            num = (int) Math.floor(Math.random() * (max - min) + min);
        } while (num > max - 1);

        return num;
    }
}

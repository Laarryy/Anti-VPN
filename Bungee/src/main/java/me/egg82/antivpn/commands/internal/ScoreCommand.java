package me.egg82.antivpn.commands.internal;

import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScoreCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommandSender sender;
    private final String source;

    private final VPNAPI api = VPNAPI.getInstance();

    private static final DecimalFormat format = new DecimalFormat(".##");

    public ScoreCommand(CommandSender sender, String source) {
        this.sender = sender;
        this.source = source;
    }

    public void run() {
        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.YELLOW + "Scoring " + ChatColor.WHITE + source + ChatColor.YELLOW + ", please wait.."));

        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.YELLOW + "Scoring against " + ChatColor.WHITE + "NordVPN" + ChatColor.YELLOW + ".."));
        test(sender, source, "NordVPN", getNordVPNIPs());
        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.YELLOW + "Sleeping for one minute.."));

        try {
            Thread.sleep(60000L);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        }

        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.YELLOW + "Scoring against " + ChatColor.WHITE + "Cryptostorm" + ChatColor.YELLOW + ".."));
        test(sender, source, "Cryptostorm", getCryptostormIPs());
        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.YELLOW + "Sleeping for one minute.."));

        try {
            Thread.sleep(60000L);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        }

        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.YELLOW + "Scoring against " + ChatColor.WHITE + "random home IPs" + ChatColor.YELLOW + ".."));
        test(sender, source, "Random home IP", getHomeIPs(), true);
        sender.sendMessage(new TextComponent(LogUtil.getHeading() + ChatColor.GREEN + "Score for " + ChatColor.YELLOW + source + ChatColor.GREEN + " complete!"));
    }

    private void test(CommandSender sender, String source, String vpnName, Set<String> ips) {
        test(sender, source, vpnName, ips, false);
    }

    private void test(CommandSender sender, String source, String vpnName, Set<String> ips, boolean flipResult) {
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
            sender.sendMessage(new TextComponent(LogUtil.getHeading() + LogUtil.getSourceHeading(source) + ChatColor.DARK_RED + "Error " + ChatColor.WHITE + format.format((error / ips.size()) * 100.0d) + "%"));
        }
        sender.sendMessage(new TextComponent(LogUtil.getHeading() + LogUtil.getSourceHeading(source) + ChatColor.YELLOW + vpnName + " score: " + ChatColor.WHITE + format.format((good / (ips.size() - error)) * 100.0d) + "%"));
    }

    private Set<String> getNordVPNIPs() {
        String[] dns = new String[] { "al9.nordvpn.com", "au306.nordvpn.com", "ca419.nordvpn.com", "ca398.nordvpn.com", "cz81.nordvpn.com", "fr345.nordvpn.com", "de114.nordvpn.com", "hk85.nordvpn.com",
                "id6.nordvpn.com", "jp224.nordvpn.com", "mx19.nordvpn.com", "nz46.nordvpn.com", "pl110.nordvpn.com", "cy9.nordvpn.com", "rs15.nordvpn.com", "kr24.nordvpn.com", "ee27.nordvpn.com",
                "at46.nordvpn.com", "uk1183.nordvpn.com", "us2580.nordvpn.com", "vn4.nordvpn.com" };
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

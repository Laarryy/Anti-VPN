package me.egg82.antivpn.commands.internal;

import co.aikar.taskchain.TaskChain;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.utils.LogUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import org.apache.commons.net.util.SubnetUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScoreCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TaskChain<?> chain;
    private final CommandSender sender;
    private final String source;

    private final VPNAPI api = VPNAPI.getInstance();

    private static final DecimalFormat format = new DecimalFormat(".##");

    public ScoreCommand(TaskChain<?> chain, CommandSender sender, String source) {
        this.chain = chain;
        this.sender = sender;
        this.source = source;
    }

    public void run() {
        sender.sendMessage(LogUtil.getHeading() + ChatColor.YELLOW + "Scoring " + ChatColor.WHITE + source + ChatColor.YELLOW + ", please wait..");

        chain
                .sync(() -> sender.sendMessage(LogUtil.getHeading() + ChatColor.YELLOW + "Scoring against " + ChatColor.WHITE + "NordVPN" + ChatColor.YELLOW + ".."))
                .async(() -> test(sender, source, "NordVPN", getNordVPNIPs()))
                .sync(() -> sender.sendMessage(LogUtil.getHeading() + ChatColor.YELLOW + "Sleeping for one minute.."))
                .delay(60, TimeUnit.SECONDS)
                .sync(() -> sender.sendMessage(LogUtil.getHeading() + ChatColor.YELLOW + "Scoring against " + ChatColor.WHITE + "Cryptostorm" + ChatColor.YELLOW + ".."))
                .async(() -> test(sender, source, "Cryptostorm", getCryptostormIPs()))
                .sync(() -> sender.sendMessage(LogUtil.getHeading() + ChatColor.YELLOW + "Sleeping for one minute.."))
                .delay(60, TimeUnit.SECONDS)
                .sync(() -> sender.sendMessage(LogUtil.getHeading() + ChatColor.YELLOW + "Scoring against " + ChatColor.WHITE + "random home IPs" + ChatColor.YELLOW + ".."))
                .async(() -> test(sender, source, "Random home IP", getHomeIPs(), true))
                .sync(() -> sender.sendMessage(LogUtil.getHeading() + ChatColor.GREEN + "Score for " + ChatColor.YELLOW + source + ChatColor.GREEN + " complete!"))
                .execute();
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

            Optional<Boolean> result = api.getResult(ip, source);
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
            sender.sendMessage(LogUtil.getHeading() + LogUtil.getSourceHeading(source) + ChatColor.DARK_RED + "Error " + ChatColor.WHITE + format.format((error / ips.size()) * 100.0d) + "%");
        }
        sender.sendMessage(LogUtil.getHeading() + LogUtil.getSourceHeading(source) + ChatColor.YELLOW + vpnName + " score: " + ChatColor.WHITE + format.format((good / (ips.size() - error)) * 100.0d) + "%");
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

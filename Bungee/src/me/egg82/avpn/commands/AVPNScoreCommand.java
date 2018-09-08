package me.egg82.avpn.commands;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.net.util.SubnetUtils;

import java.net.InetAddress;
import java.text.DecimalFormat;

import me.egg82.avpn.Config;
import me.egg82.avpn.VPNAPI;
import me.egg82.avpn.debug.IDebugPrinter;
import me.egg82.avpn.utils.ValidationUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.handlers.async.AsyncCommandHandler;
import ninja.egg82.utils.MathUtil;
import ninja.egg82.utils.ThreadUtil;

public class AVPNScoreCommand extends AsyncCommandHandler {
    // vars
    private static DecimalFormat format = new DecimalFormat(".##");

    private VPNAPI api = VPNAPI.getInstance();

    // constructor
    public AVPNScoreCommand() {
        super();
    }

    // public

    // private
    protected void onExecute(long elapsedMilliseconds) {
        if (!sender.hasPermission("avpn.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return;
        }
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Incorrect command usage!");
            String name = getClass().getSimpleName();
            name = name.substring(0, name.length() - 7).toLowerCase();
            ServiceLocator.getService(Plugin.class).getProxy().getPluginManager().dispatchCommand((CommandSender) sender.getHandle(), "? " + name);
            return;
        }

        String source = args[0].toLowerCase();

        ThreadUtil.submit(new Runnable() {
            public void run() {
                test(source, "NordVPN", getNordVPNIPs());
                sender.sendMessage(ChatColor.YELLOW + "Sleeping for one minute..");
                try {
                    Thread.sleep(60000L);
                } catch (Exception ex) {

                }
                test(source, "Cryptostorm", getCryptostormIPs());
                sender.sendMessage(ChatColor.YELLOW + "Sleeping for one minute..");
                try {
                    Thread.sleep(60000L);
                } catch (Exception ex) {

                }
                test(source, "Random Home IPs", getHomeIPs(), true);

                sender.sendMessage(ChatColor.GREEN + "Score complete!");
            }
        });
    }

    protected void onUndo() {

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
        Set<String> retVal = new HashSet<String>();

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
        Set<String> retVal = new HashSet<String>();

        for (String name : dns) {
            try {
                InetAddress address = InetAddress.getByName(name);
                String ip = address.getHostAddress();
                if (ValidationUtil.isValidIp(ip)) {
                    retVal.add(ip);
                } else {
                    if (Config.debug) {
                        ServiceLocator.getService(IDebugPrinter.class).printInfo(name + " does not have a valid IP: " + ip);
                    }
                }
            } catch (Exception ex) {
                if (Config.debug) {
                    ServiceLocator.getService(IDebugPrinter.class).printInfo(name + " could not be fetched.");
                }
            }
        }

        return retVal;
    }

    private Set<String> getIPs(String mask, int count) {
        SubnetUtils subnet = new SubnetUtils(mask);
        String[] addresses = subnet.getInfo().getAllAddresses();

        Set<String> retVal = new HashSet<String>();
        while (retVal.size() < count) {
            retVal.add(addresses[MathUtil.fairRoundedRandom(0, addresses.length - 1)]);
        }

        return retVal;
    }

    private void test(String source, String vpnName, Set<String> ips) {
        test(source, vpnName, ips, false);
    }

    private void test(String source, String vpnName, Set<String> ips, boolean flipResult) {
        double error = 0.0d;
        double good = 0.0d;

        sender.sendMessage(ChatColor.YELLOW + "Scoring against " + vpnName + "..");
        for (String ip : ips) {
            try {
                Thread.sleep(5000L);
            } catch (Exception ex) {

            }

            Optional<Boolean> result = api.getResult(ip, source);
            Boolean bool = result.orElse(null);
            if (bool == null) {
                error++;
                continue;
            }

            if (!flipResult && bool.booleanValue()) {
                good++;
            } else if (flipResult && !bool.booleanValue()) {
                good++;
            }
        }
        if (error > 0) {
            sender.sendMessage(ChatColor.YELLOW + "Source error: " + format.format((error / ips.size()) * 100.0d) + "%");
        }
        sender.sendMessage(ChatColor.YELLOW + vpnName + " score: " + format.format((good / (ips.size() - error)) * 100.0d) + "%");
    }
}

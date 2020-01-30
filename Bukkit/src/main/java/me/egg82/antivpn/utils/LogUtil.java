package me.egg82.antivpn.utils;

import org.bukkit.ChatColor;

public class LogUtil {
    private LogUtil() {}

    public static String getHeading() { return ChatColor.YELLOW + "[" + ChatColor.AQUA + "Anti-VPN" + ChatColor.YELLOW + "] " + ChatColor.RESET; }
}

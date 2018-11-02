package me.egg82.antivpn.utils;

import org.bukkit.ChatColor;

public class LogUtil {
    public static String getHeading() { return ChatColor.YELLOW + "[" + ChatColor.AQUA + "Anti-VPN" + ChatColor.YELLOW + "] " + ChatColor.RESET; }

    public static String getSourceHeading(String source) { return ChatColor.YELLOW + "[" + ChatColor.LIGHT_PURPLE + source + ChatColor.YELLOW + "] " + ChatColor.RESET; }
}

package me.egg82.antivpn.utils;

import org.bukkit.ChatColor;

public class LogUtil {
    private LogUtil() {}

    public static String getHeading() { return ChatColor.YELLOW + "[" + ChatColor.AQUA + "AntiVPN" + ChatColor.YELLOW + "] " + ChatColor.RESET; }
}

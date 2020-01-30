package me.egg82.antivpn.utils;

import net.md_5.bungee.api.ChatColor;

public class LogUtil {
    private LogUtil() {}

    public static String getHeading() { return ChatColor.YELLOW + "[" + ChatColor.AQUA + "Anti-VPN" + ChatColor.YELLOW + "] " + ChatColor.RESET; }
}

package me.egg82.antivpn.utils;

public class BukkitEnvironmentUtil {
    private BukkitEnvironmentUtil() { }

    private static Environment environemnt;
    public static Environment getEnvironment() { return environemnt; }

    static {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            environemnt = Environment.PAPER;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("org.spigotmc.SpigotConfig");
                environemnt = Environment.SPIGOT;
            } catch (ClassNotFoundException e1) {
                environemnt = Environment.BUKKIT;
            }
        }
    }

    public enum Environment {
        PAPER,
        SPIGOT,
        BUKKIT;
    }
}

package me.egg82.antivpn.bukkit;

import org.checkerframework.checker.nullness.qual.NonNull;

public class BukkitEnvironmentUtil {
    private BukkitEnvironmentUtil() { }

    private static Environment environment;
    public static @NonNull Environment getEnvironment() { return environment; }

    static {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            environment = Environment.PAPER;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("org.spigotmc.SpigotConfig");
                environment = Environment.SPIGOT;
            } catch (ClassNotFoundException e1) {
                environment = Environment.BUKKIT;
            }
        }
    }

    public enum Environment {
        PAPER,
        SPIGOT,
        BUKKIT;
    }
}

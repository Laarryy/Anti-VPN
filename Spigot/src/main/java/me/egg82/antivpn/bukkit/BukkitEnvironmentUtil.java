package me.egg82.antivpn.bukkit;

import org.jetbrains.annotations.NotNull;

public class BukkitEnvironmentUtil {
    private BukkitEnvironmentUtil() { }

    private static Environment environment;

    public static @NotNull Environment getEnvironment() { return environment; }

    static {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            environment = Environment.SPIGOT;
        } catch (ClassNotFoundException e) {
            environment = Environment.BUKKIT;
        }
    }

    public enum Environment {
        SPIGOT,
        BUKKIT
    }
}

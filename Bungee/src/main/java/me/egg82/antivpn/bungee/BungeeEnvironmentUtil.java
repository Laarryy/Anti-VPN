package me.egg82.antivpn.bungee;

import org.jetbrains.annotations.NotNull;

public class BungeeEnvironmentUtil {
    private BungeeEnvironmentUtil() { }

    private static Environment environemnt;
    public static @NotNull Environment getEnvironment() { return environemnt; }

    static {
        try {
            Class.forName("io.github.waterfallmc.waterfall.conf.WaterfallConfiguration");
            environemnt = Environment.WATERFALL;
        } catch (ClassNotFoundException e) {
            environemnt = Environment.BUNGEECORD;
        }
    }

    public enum Environment {
        WATERFALL,
        BUNGEECORD;
    }
}

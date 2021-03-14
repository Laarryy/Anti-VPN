package me.egg82.antivpn.bukkit;

public class BukkitCapabilities {
    public static final boolean HAS_ADVENTURE;

    static {
        boolean hasAdventure;
        try {
            Class.forName("io.papermc.paper.adventure.PaperAdventure");
            hasAdventure = true;
        } catch (ClassNotFoundException e) {
            hasAdventure = false;
        }
        HAS_ADVENTURE = hasAdventure;
    }

    private BukkitCapabilities() { }
}

package me.egg82.antivpn.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class VelocityLogUtil {
    private VelocityLogUtil() { }

    public static final TextComponent HEADING = Component.text("[", NamedTextColor.YELLOW)
            .append(Component.text("Anti-VPN", NamedTextColor.AQUA))
            .append(Component.text("] ", NamedTextColor.YELLOW));
}

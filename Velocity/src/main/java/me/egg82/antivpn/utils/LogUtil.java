package me.egg82.antivpn.utils;

import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

public class LogUtil {
    private LogUtil() {}

    public static TextComponent.Builder getHeading() {
        return TextComponent.builder("[").color(TextColor.YELLOW)
                .append(TextComponent.of("AntiVPN").color(TextColor.AQUA))
                .append(TextComponent.of("] ").color(TextColor.YELLOW));
    }
}

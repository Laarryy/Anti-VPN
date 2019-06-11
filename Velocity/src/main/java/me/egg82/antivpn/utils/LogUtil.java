package me.egg82.antivpn.utils;

import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

public class LogUtil {
    private LogUtil() {}

    public static TextComponent.Builder getHeading() {
        return TextComponent.builder("[").color(TextColor.YELLOW)
                .append(TextComponent.of("Anti-VPN").color(TextColor.AQUA))
                .append(TextComponent.of("] ").color(TextColor.YELLOW));
    }

    public static TextComponent getSourceHeading(String source) {
        return TextComponent.builder("[").color(TextColor.YELLOW)
                .append(TextComponent.of(source).color(TextColor.LIGHT_PURPLE))
                .append(TextComponent.of("] ").color(TextColor.YELLOW))
                .build();
    }
}

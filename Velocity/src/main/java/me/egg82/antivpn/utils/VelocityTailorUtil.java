package me.egg82.antivpn.utils;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VelocityTailorUtil {
    private static final Logger logger = LoggerFactory.getLogger(VelocityTailorUtil.class);

    private VelocityTailorUtil() { }

    public static @NotNull List<String> tailorCommands(@NotNull List<String> commands, @NotNull String name, @NotNull UUID uuid, @NotNull String ip) {
        List<String> retVal = new ArrayList<>();

        for (String command : commands) {
            command = command.replace("%player%", name).replace("%uuid%", uuid.toString()).replace("%ip%", ip);
            if (command.charAt(0) == '/') {
                command = command.substring(1);
            }

            retVal.add(command);
        }

        return retVal;
    }

    public static @NotNull String tailorKickMessage(@NotNull String message, @NotNull String name, @NotNull UUID uuid, @NotNull String ip) {
        message = message.replace("%player%", name).replace("%uuid%", uuid.toString()).replace("%ip%", ip);
        return message.replace("\\r", "").replace("\r", "").replace("\\n", "\n");
    }
}

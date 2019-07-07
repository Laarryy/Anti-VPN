package me.egg82.antivpn.commands.internal;

import com.velocitypowered.api.command.CommandSource;
import java.util.Map;
import java.util.Optional;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.utils.LogUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommandSource source;
    private final String ip;

    private final VPNAPI api = VPNAPI.getInstance();

    public TestCommand(CommandSource source, String ip) {
        this.source = source;
        this.ip = ip;
    }

    public void run() {
        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Testing with ").color(TextColor.YELLOW)).append(TextComponent.of(ip).color(TextColor.WHITE)).append(TextComponent.of(", please wait..").color(TextColor.YELLOW)).build());

        Map<String, Optional<Boolean>> results;
        try {
            results = api.testAllSources(ip);
        } catch (APIException ex) {
            logger.error(ex.getMessage(), ex);
            source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Internal error").color(TextColor.DARK_RED)).build());
            return;
        }

        for (Map.Entry<String, Optional<Boolean>> kvp : results.entrySet()) {
            if (!kvp.getValue().isPresent()) {
                source.sendMessage(LogUtil.getHeading().append(LogUtil.getSourceHeading(kvp.getKey())).append(TextComponent.of("Source error").color(TextColor.YELLOW)).build());
                continue;
            }

            source.sendMessage(LogUtil.getHeading().append(LogUtil.getSourceHeading(kvp.getKey())).append(TextComponent.of(kvp.getValue().get() ? "VPN/Proxy detected" : "No VPN/Proxy detected").color(kvp.getValue().get() ? TextColor.DARK_RED : TextColor.GREEN)).build());
        }
        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Test for ").color(TextColor.GREEN)).append(TextComponent.of(ip).color(TextColor.YELLOW)).append(TextComponent.of(" complete!").color(TextColor.GREEN)).build());
    }
}

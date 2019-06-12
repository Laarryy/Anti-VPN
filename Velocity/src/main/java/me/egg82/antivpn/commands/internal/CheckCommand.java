package me.egg82.antivpn.commands.internal;

import com.velocitypowered.api.command.CommandSource;
import java.util.Optional;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.LogUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommandSource source;
    private final String ip;

    private final VPNAPI api = VPNAPI.getInstance();

    public CheckCommand(CommandSource source, String ip) {
        this.source = source;
        this.ip = ip;
    }

    public void run() {
        source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Checking ").color(TextColor.YELLOW)).append(TextComponent.of(ip).color(TextColor.WHITE)).append(TextComponent.of("..").color(TextColor.YELLOW)).build());

        Optional<Configuration> config = ConfigUtil.getConfig();
        if (!config.isPresent()) {
            source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Internal error").color(TextColor.DARK_RED)).build());
            return;
        }

        Optional<Boolean> isVPN = Optional.empty();
        if (config.get().getNode("kick", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
            double consensus = clamp(0.0d, 1.0d, config.get().getNode("kick", "algorithm", "min-consensus").getDouble(0.6d));
            try {
                isVPN = Optional.of(api.consensus(ip) >= consensus);
            } catch (APIException ex) {
                logger.error(ex.getMessage(), ex);
            }
        } else {
            try {
                isVPN = Optional.of(api.cascade(ip));
            } catch (APIException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        if (!isVPN.isPresent()) {
            source.sendMessage(LogUtil.getHeading().append(TextComponent.of("Internal error").color(TextColor.DARK_RED)).build());
            return;
        }

        if (isVPN.get()) {
            source.sendMessage(LogUtil.getHeading().append(TextComponent.of("VPN/PRoxy detected").color(TextColor.DARK_RED)).build());
        } else {
            source.sendMessage(LogUtil.getHeading().append(TextComponent.of("No VPN/Proxy detected").color(TextColor.GREEN)).build());
        }
    }

    private double clamp(double min, double max, double val) { return Math.min(max, Math.max(min, val)); }
}

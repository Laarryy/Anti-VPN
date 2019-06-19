package me.egg82.antivpn.events;

import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.function.Consumer;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.services.AnalyticsHelper;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.LogUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostLoginCheckHandler implements Consumer<PostLoginEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProxyServer proxy;

    private final VPNAPI api = VPNAPI.getInstance();

    public PostLoginCheckHandler(ProxyServer proxy) {
        this.proxy = proxy;
    }

    public void accept(PostLoginEvent event) {
        String ip = getIp(event.getPlayer().getRemoteAddress());
        if (ip == null || ip.isEmpty()) {
            return;
        }

        Optional<Configuration> config = ConfigUtil.getConfig();
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!config.isPresent() || !cachedConfig.isPresent()) {
            return;
        }

        if (event.getPlayer().hasPermission("avpn.bypass")) {
            if (ConfigUtil.getDebugOrFalse()) {
                proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of(event.getPlayer().getUsername()).color(TextColor.WHITE)).append(TextComponent.of(" bypasses check. Ignoring.").color(TextColor.YELLOW)).build());
            }
            return;
        }

        if (config.get().getNode("action", "kick-message").getString("").isEmpty() && config.get().getNode("action", "command").getString("").isEmpty()) {
            if (ConfigUtil.getDebugOrFalse()) {
                proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of("Plugin set to API-only. Ignoring ").color(TextColor.YELLOW)).append(TextComponent.of(event.getPlayer().getUsername()).color(TextColor.WHITE)).build());
            }
            return;
        }

        if (cachedConfig.get().getIgnoredIps().contains(ip)) {
            return;
        }

        boolean isVPN;

        if (config.get().getNode("action", "algorithm", "method").getString("cascade").equalsIgnoreCase("consensus")) {
            double consensus = clamp(0.0d, 1.0d, config.get().getNode("action", "algorithm", "min-consensus").getDouble(0.6d));
            try {
                isVPN = api.consensus(ip) >= consensus;
            } catch (APIException ex) {
                logger.error(ex.getMessage(), ex);
                isVPN = false;
            }
        } else {
            try {
                isVPN = api.cascade(ip);
            } catch (APIException ex) {
                logger.error(ex.getMessage(), ex);
                isVPN = false;
            }
        }

        if (isVPN) {
            AnalyticsHelper.incrementBlocked();
            if (ConfigUtil.getDebugOrFalse()) {
                proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of(event.getPlayer().getUsername()).color(TextColor.WHITE)).append(TextComponent.of(" found using a VPN. Running required actions.").color(TextColor.DARK_RED)).build());
            }

            tryRunCommand(config.get(), event.getPlayer());
            tryKickPlayer(config.get(), event.getPlayer());
        } else {
            proxy.getConsoleCommandSource().sendMessage(LogUtil.getHeading().append(TextComponent.of(event.getPlayer().getUsername()).color(TextColor.WHITE)).append(TextComponent.of(" passed VPN check.").color(TextColor.GREEN)).build());
        }
    }

    private void tryRunCommand(Configuration config, Player player) {
        String command = config.getNode("action", "command").getString("");
        if (command.isEmpty()) {
            return;
        }

        command = command.replace("%player%", player.getUsername()).replace("%uuid%", player.getUniqueId().toString());
        if (command.charAt(0) == '/') {
            command = command.substring(1);
        }

        proxy.getCommandManager().execute(proxy.getConsoleCommandSource(), command);
    }

    private void tryKickPlayer(Configuration config, Player player) {
        String message = config.getNode("action", "kick-message").getString("");
        if (message.isEmpty()) {
            return;
        }

        player.disconnect(TextComponent.of(message));
    }

    private String getIp(InetSocketAddress address) {
        if (address == null) {
            return null;
        }

        InetAddress host = address.getAddress();
        if (host == null) {
            return null;
        }

        return host.getHostAddress();
    }

    private double clamp(double min, double max, double val) { return Math.min(max, Math.max(min, val)); }
}

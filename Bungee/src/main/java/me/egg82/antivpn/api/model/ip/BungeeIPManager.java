package me.egg82.antivpn.api.model.ip;

import com.google.common.collect.ImmutableList;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.BungeeTailorUtil;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BungeeIPManager extends AbstractIPManager {
    public BungeeIPManager(@NotNull SourceManager sourceManager, long cacheTime, @NotNull TimeUnit cacheTimeUnit) {
        super(sourceManager, cacheTime, cacheTimeUnit);
    }

    @Override
    public boolean kickForVpn(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(playerUuid);
        if (p == null) {
            return false;
        }

        List<String> commands = BungeeTailorUtil.tailorCommands(cachedConfig.getVPNActionCommands(), playerName, playerUuid, ip);
        for (String command : commands) {
            ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), command);
        }
        if (!cachedConfig.getVPNKickMessage().isEmpty()) {
            p.disconnect(TextComponent.fromLegacyText(BungeeTailorUtil.tailorKickMessage(cachedConfig.getVPNKickMessage(), playerName, playerUuid, ip)));
        }
        return true;
    }

    @Override
    public @Nullable String getVpnKickMessage(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (!cachedConfig.getVPNKickMessage().isEmpty()) {
            return BungeeTailorUtil.tailorKickMessage(cachedConfig.getVPNKickMessage(), playerName, playerUuid, ip);
        }
        return null;
    }

    @Override
    public @NotNull List<String> getVpnCommands(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (!cachedConfig.getVPNActionCommands().isEmpty()) {
            return ImmutableList.copyOf(BungeeTailorUtil.tailorCommands(cachedConfig.getVPNActionCommands(), playerName, playerUuid, ip));
        }
        return ImmutableList.of();
    }
}

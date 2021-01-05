package me.egg82.antivpn.api.model.ip;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.BungeeTailorUtil;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BungeeIPManager extends AbstractIPManager {
    public BungeeIPManager(@NonNull SourceManager sourceManager, long cacheTime, TimeUnit cacheTimeUnit) {
        super(sourceManager, cacheTime, cacheTimeUnit);
    }

    public boolean kickForVpn(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            return false;
        }

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

    public @Nullable String getVpnKickMessage(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            return null;
        }

        if (!cachedConfig.getVPNKickMessage().isEmpty()) {
            return BungeeTailorUtil.tailorKickMessage(cachedConfig.getVPNKickMessage(), playerName, playerUuid, ip);
        }
        return null;
    }

    public @NonNull List<String> getVpnCommands(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            return ImmutableList.of();
        }

        if (!cachedConfig.getVPNActionCommands().isEmpty()) {
            return ImmutableList.copyOf(BungeeTailorUtil.tailorCommands(cachedConfig.getVPNActionCommands(), playerName, playerUuid, ip));
        }
        return ImmutableList.of();
    }
}

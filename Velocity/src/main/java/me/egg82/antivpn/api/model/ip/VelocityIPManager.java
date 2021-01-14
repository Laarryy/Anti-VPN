package me.egg82.antivpn.api.model.ip;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.VelocityTailorUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityIPManager extends AbstractIPManager {
    private final ProxyServer proxy;

    public VelocityIPManager(@NonNull ProxyServer proxy, @NonNull SourceManager sourceManager, long cacheTime, TimeUnit cacheTimeUnit) {
        super(sourceManager, cacheTime, cacheTimeUnit);
        this.proxy = proxy;
    }

    public boolean kickForVpn(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            return false;
        }

        Optional<Player> p = proxy.getPlayer(playerUuid);
        if (!p.isPresent()) {
            return false;
        }

        List<String> commands = VelocityTailorUtil.tailorCommands(cachedConfig.getVPNActionCommands(), playerName, playerUuid, ip);
        for (String command : commands) {
            proxy.getCommandManager().executeImmediatelyAsync(proxy.getConsoleCommandSource(), command);
        }
        if (!cachedConfig.getVPNKickMessage().isEmpty()) {
            p.get().disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(VelocityTailorUtil.tailorKickMessage(cachedConfig.getVPNKickMessage(), playerName, playerUuid, ip)));
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
            return VelocityTailorUtil.tailorKickMessage(cachedConfig.getVPNKickMessage(), playerName, playerUuid, ip);
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
            return ImmutableList.copyOf(VelocityTailorUtil.tailorCommands(cachedConfig.getVPNActionCommands(), playerName, playerUuid, ip));
        }
        return ImmutableList.of();
    }
}

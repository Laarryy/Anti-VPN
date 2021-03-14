package me.egg82.antivpn.api.model.ip;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.VelocityTailorUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VelocityIPManager extends AbstractIPManager {
    private final ProxyServer proxy;

    public VelocityIPManager(@NotNull ProxyServer proxy, @NotNull SourceManager sourceManager, long cacheTime, TimeUnit cacheTimeUnit) {
        super(sourceManager, cacheTime, cacheTimeUnit);
        this.proxy = proxy;
    }

    @Override
    public boolean kickForVpn(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        Optional<Player> p = proxy.getPlayer(playerUuid);
        if (!p.isPresent()) {
            return false;
        }

        List<String> commands = VelocityTailorUtil.tailorCommands(cachedConfig.getVPNActionCommands(), playerName, playerUuid, ip);
        for (String command : commands) {
            proxy.getCommandManager().executeImmediatelyAsync(proxy.getConsoleCommandSource(), command);
        }
        if (!cachedConfig.getVPNKickMessage().isEmpty()) {
            p.get()
                    .disconnect(LegacyComponentSerializer.legacyAmpersand()
                                        .deserialize(VelocityTailorUtil.tailorKickMessage(cachedConfig.getVPNKickMessage(), playerName, playerUuid, ip)));
        }
        return true;
    }

    @Override
    public @Nullable String getVpnKickMessage(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (!cachedConfig.getVPNKickMessage().isEmpty()) {
            return VelocityTailorUtil.tailorKickMessage(cachedConfig.getVPNKickMessage(), playerName, playerUuid, ip);
        }
        return null;
    }

    @Override
    public @NotNull List<String> getVpnCommands(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (!cachedConfig.getVPNActionCommands().isEmpty()) {
            return ImmutableList.copyOf(VelocityTailorUtil.tailorCommands(cachedConfig.getVPNActionCommands(), playerName, playerUuid, ip));
        }
        return ImmutableList.of();
    }
}

package me.egg82.antivpn.api.model.ip;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.BukkitCommandUtil;
import me.egg82.antivpn.utils.BukkitTailorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BukkitIPManager extends AbstractIPManager {
    private final Plugin plugin;

    public BukkitIPManager(@NonNull Plugin plugin, @NonNull SourceManager sourceManager, long cacheTime, TimeUnit cacheTimeUnit) {
        super(sourceManager, cacheTime, cacheTimeUnit);
        this.plugin = plugin;
    }

    public boolean kickForVpn(@NonNull String playerName, @NonNull UUID playerUuid, @NonNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            return false;
        }

        Player p = Bukkit.getPlayer(playerUuid);
        if (p == null) {
            return false;
        }

        BukkitCommandUtil.dispatchCommands(BukkitTailorUtil.tailorCommands(cachedConfig.getVPNActionCommands(), playerName, playerUuid, ip), Bukkit.getConsoleSender(), plugin);
        if (!cachedConfig.getVPNKickMessage().isEmpty()) {
            p.kickPlayer(BukkitTailorUtil.tailorKickMessage(cachedConfig.getVPNKickMessage(), playerName, playerUuid, ip));
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
            return BukkitTailorUtil.tailorKickMessage(cachedConfig.getVPNKickMessage(), playerName, playerUuid, ip);
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
            return ImmutableList.copyOf(BukkitTailorUtil.tailorCommands(cachedConfig.getVPNActionCommands(), playerName, playerUuid, ip));
        }
        return ImmutableList.of();
    }
}

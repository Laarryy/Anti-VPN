package me.egg82.antivpn.api.model.player;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.storage.models.PlayerModel;
import me.egg82.antivpn.utils.BukkitCommandUtil;
import me.egg82.antivpn.utils.BukkitTailorUtil;
import me.egg82.antivpn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitPlayerManager extends AbstractPlayerManager {
    private final Plugin plugin;

    public BukkitPlayerManager(@NotNull Plugin plugin, @Nullable String mcleaksKey, @NotNull TimeUtil.Time cacheTime) {
        super(cacheTime, mcleaksKey);

        this.plugin = plugin;
    }

    public @NotNull CompletableFuture<@Nullable Player> getPlayer(@NotNull UUID uniqueId) {
        return CompletableFuture.supplyAsync(() -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

            for (StorageService service : cachedConfig.getStorage()) {
                PlayerModel model = service.getPlayerModel(uniqueId, cachedConfig.getSourceCacheTime());
                if (model != null) {
                    return new BukkitPlayer(uniqueId, model.isMcleaks());
                }
            }
            return null;
        });
    }

    public @NotNull CompletableFuture<@Nullable Player> getPlayer(@NotNull String username) {
        return PlayerLookup.get(username).thenApply(info -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

            for (StorageService service : cachedConfig.getStorage()) {
                PlayerModel model = service.getPlayerModel(info.getUUID(), cachedConfig.getSourceCacheTime());
                if (model != null) {
                    return new BukkitPlayer(info.getUUID(), model.isMcleaks());
                }
            }
            return null;
        });
    }

    public boolean kickForMcLeaks(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        org.bukkit.entity.Player p = Bukkit.getPlayer(playerUuid);
        if (p == null) {
            return false;
        }

        BukkitCommandUtil.dispatchCommands(BukkitTailorUtil.tailorCommands(cachedConfig.getMCLeaksActionCommands(), playerName, playerUuid, ip), Bukkit.getConsoleSender(), plugin);
        if (!cachedConfig.getMCLeaksKickMessage().isEmpty()) {
            p.kickPlayer(BukkitTailorUtil.tailorKickMessage(cachedConfig.getMCLeaksKickMessage(), playerName, playerUuid, ip));
        }
        return true;
    }

    public @Nullable String getMcLeaksKickMessage(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (!cachedConfig.getMCLeaksKickMessage().isEmpty()) {
            return BukkitTailorUtil.tailorKickMessage(cachedConfig.getMCLeaksKickMessage(), playerName, playerUuid, ip);
        }
        return null;
    }

    public @NotNull List<@NotNull String> getMcLeaksCommands(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (!cachedConfig.getMCLeaksActionCommands().isEmpty()) {
            return ImmutableList.copyOf(BukkitTailorUtil.tailorCommands(cachedConfig.getMCLeaksActionCommands(), playerName, playerUuid, ip));
        }
        return ImmutableList.of();
    }
}

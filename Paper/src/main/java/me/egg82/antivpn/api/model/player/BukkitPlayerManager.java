package me.egg82.antivpn.api.model.player;

import com.google.common.collect.ImmutableList;
import me.egg82.antivpn.bukkit.BukkitCapabilities;
import me.egg82.antivpn.bukkit.BukkitCommandUtil;
import me.egg82.antivpn.bukkit.BukkitTailorUtil;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.storage.models.PlayerModel;
import me.egg82.antivpn.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.craftbukkit.BukkitComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitPlayerManager extends AbstractPlayerManager {
    private final Plugin plugin;

    public BukkitPlayerManager(@NotNull Plugin plugin, @Nullable String mcleaksKey, @NotNull TimeUtil.Time cacheTime) {
        super(cacheTime, mcleaksKey);

        this.plugin = plugin;
    }

    @Override
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

    @Override
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

    @Override
    @SuppressWarnings("deprecation")
    public boolean kickForMcLeaks(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        org.bukkit.entity.Player p = Bukkit.getPlayer(playerUuid);
        if (p == null) {
            return false;
        }

        BukkitCommandUtil.dispatchCommands(
                BukkitTailorUtil.tailorCommands(cachedConfig.getMCLeaksActionCommands(), playerName, playerUuid, ip),
                Bukkit.getConsoleSender(),
                plugin
        );
        if (!cachedConfig.getMCLeaksKickMessage().isEmpty()) {
            if (BukkitCapabilities.HAS_ADVENTURE) {
                p.kick(BukkitTailorUtil.tailorKickMessage(cachedConfig.getMCLeaksKickMessage(), playerName, playerUuid, ip));
            } else {
                p.kickPlayer(BukkitComponentSerializer.legacy()
                                     .serialize(BukkitTailorUtil.tailorKickMessage(cachedConfig.getMCLeaksKickMessage(), playerName, playerUuid, ip)));
            }
        }
        return true;
    }

    @Override
    public @Nullable Component getMcLeaksKickMessage(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (!cachedConfig.getMCLeaksKickMessage().isEmpty()) {
            return BukkitTailorUtil.tailorKickMessage(cachedConfig.getMCLeaksKickMessage(), playerName, playerUuid, ip);
        }
        return null;
    }

    @Override
    public @NotNull List<@NotNull String> getMcLeaksCommands(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (!cachedConfig.getMCLeaksActionCommands().isEmpty()) {
            return ImmutableList.copyOf(BukkitTailorUtil.tailorCommands(cachedConfig.getMCLeaksActionCommands(), playerName, playerUuid, ip));
        }
        return ImmutableList.of();
    }
}

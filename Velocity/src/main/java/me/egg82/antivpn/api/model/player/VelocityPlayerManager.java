package me.egg82.antivpn.api.model.player;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.messaging.packets.vpn.PlayerPacket;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.storage.models.PlayerModel;
import me.egg82.antivpn.utils.PacketUtil;
import me.egg82.antivpn.utils.VelocityTailorUtil;
import me.gong.mcleaks.MCLeaksAPI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class VelocityPlayerManager extends AbstractPlayerManager {
    private final ProxyServer proxy;
    private final MCLeaksAPI api;

    public VelocityPlayerManager(@NotNull ProxyServer proxy, int webThreads, String mcleaksKey, long cacheTime, TimeUnit cacheTimeUnit) {
        super(cacheTime, cacheTimeUnit);

        this.proxy = proxy;
        api = MCLeaksAPI.builder()
                .nocache()
                .threadCount(webThreads)
                .userAgent("egg82/Anti-VPN")
                .apiKey(mcleaksKey)
                .build();
    }

    public void cancel() {
        api.shutdown();
    }

    @Override
    public @NotNull CompletableFuture<Player> getPlayer(@NotNull UUID uniqueId) {
        return CompletableFuture.supplyAsync(() -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

            for (StorageService service : cachedConfig.getStorage()) {
                PlayerModel model = service.getPlayerModel(uniqueId, cachedConfig.getSourceCacheTime());
                if (model != null) {
                    return new VelocityPlayer(proxy, uniqueId, model.isMcleaks());
                }
            }
            return null;
        });
    }

    @Override
    public @NotNull CompletableFuture<Player> getPlayer(@NotNull String username) {
        return PlayerLookup.get(username, proxy).thenApply(info -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

            for (StorageService service : cachedConfig.getStorage()) {
                PlayerModel model = service.getPlayerModel(info.getUUID(), cachedConfig.getSourceCacheTime());
                if (model != null) {
                    return new VelocityPlayer(proxy, info.getUUID(), model.isMcleaks());
                }
            }
            return null;
        });
    }

    @Override
    public boolean kickForMcLeaks(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        Optional<com.velocitypowered.api.proxy.Player> p = proxy.getPlayer(playerUuid);
        if (!p.isPresent()) {
            return false;
        }

        List<String> commands = VelocityTailorUtil.tailorCommands(cachedConfig.getMCLeaksActionCommands(), playerName, playerUuid, ip);
        for (String command : commands) {
            proxy.getCommandManager().executeImmediatelyAsync(proxy.getConsoleCommandSource(), command);
        }
        if (!cachedConfig.getMCLeaksKickMessage().isEmpty()) {
            p.get()
                    .disconnect(LegacyComponentSerializer.legacyAmpersand()
                                        .deserialize(VelocityTailorUtil.tailorKickMessage(cachedConfig.getMCLeaksKickMessage(), playerName, playerUuid, ip)));
        }
        return true;
    }

    @Override
    public @Nullable String getMcLeaksKickMessage(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (!cachedConfig.getMCLeaksKickMessage().isEmpty()) {
            return VelocityTailorUtil.tailorKickMessage(cachedConfig.getMCLeaksKickMessage(), playerName, playerUuid, ip);
        }
        return null;
    }

    @Override
    public @NotNull List<String> getMcLeaksCommands(@NotNull String playerName, @NotNull UUID playerUuid, @NotNull String ip) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (!cachedConfig.getMCLeaksActionCommands().isEmpty()) {
            return ImmutableList.copyOf(VelocityTailorUtil.tailorCommands(cachedConfig.getMCLeaksActionCommands(), playerName, playerUuid, ip));
        }
        return ImmutableList.of();
    }

    protected @NotNull PlayerModel calculatePlayerResult(@NotNull UUID uuid, boolean useCache) throws APIException {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (useCache) {
            for (StorageService service : cachedConfig.getStorage()) {
                PlayerModel model = service.getPlayerModel(uuid, cachedConfig.getSourceCacheTime());
                if (model != null) {
                    if (cachedConfig.getDebug()) {
                        logger.info("Found database value for player " + uuid + ".");
                    }
                    return model;
                }
            }
        }

        if (cachedConfig.getDebug()) {
            logger.info("Getting web result for player " + uuid + ".");
        }

        PlayerModel retVal = new PlayerModel();
        retVal.setUuid(uuid);

        MCLeaksAPI.Result result = api.checkAccount(uuid);
        if (result.hasError()) {
            throw new APIException(result.getError().getMessage().contains("key"), result.getError());
        }

        retVal.setMcleaks(result.isMCLeaks());

        if (useCache) {
            storeResult(retVal, cachedConfig);
            sendResult(retVal, cachedConfig);
        }
        return retVal;
    }

    private void storeResult(@NotNull PlayerModel model, @NotNull CachedConfig cachedConfig) {
        for (StorageService service : cachedConfig.getStorage()) {
            PlayerModel m = service.getOrCreatePlayerModel(model.getUuid(), model.isMcleaks());
            service.storeModel(m);
        }

        if (cachedConfig.getDebug()) {
            logger.info("Stored data for " + model.getUuid() + " in storage.");
        }
    }

    private void sendResult(@NotNull PlayerModel model, @NotNull CachedConfig cachedConfig) {
        PlayerPacket packet = new PlayerPacket();
        packet.setUuid(model.getUuid());
        packet.setValue(model.isMcleaks());
        PacketUtil.queuePacket(packet);

        if (cachedConfig.getDebug()) {
            logger.info("Queued packet for " + model.getUuid() + " in messaging.");
        }
    }
}

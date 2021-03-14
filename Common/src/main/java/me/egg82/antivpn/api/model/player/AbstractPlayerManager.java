package me.egg82.antivpn.api.model.player;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import flexjson.JSONDeserializer;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.messaging.packets.vpn.DeletePlayerPacket;
import me.egg82.antivpn.messaging.packets.vpn.PlayerPacket;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.storage.models.PlayerModel;
import me.egg82.antivpn.utils.PacketUtil;
import me.egg82.antivpn.utils.TimeUtil;
import me.egg82.antivpn.web.WebRequest;
import me.egg82.antivpn.web.models.MCLeaksResultModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public abstract class AbstractPlayerManager implements PlayerManager {
    protected final Logger logger = new GELFLogger(LoggerFactory.getLogger(getClass()));

    protected final LoadingCache<UUID, PlayerModel> playerCache;
    private final String mcleaksKey;

    protected AbstractPlayerManager(@NotNull TimeUtil.Time cacheTime, @Nullable String mcleaksKey) {
        playerCache = Caffeine.newBuilder()
                .expireAfterAccess(cacheTime.getTime(), cacheTime.getUnit())
                .expireAfterWrite(cacheTime.getTime(), cacheTime.getUnit())
                .build(k -> calculatePlayerResult(k, true));
        this.mcleaksKey = mcleaksKey;
    }

    public LoadingCache<UUID, PlayerModel> getPlayerCache() {
        return playerCache;
    }

    public @NotNull CompletableFuture<Void> savePlayer(@NotNull Player player) {
        return CompletableFuture.runAsync(() -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

            for (StorageService service : cachedConfig.getStorage()) {
                PlayerModel model = service.getOrCreatePlayerModel(player.getUuid(), player.isMcLeaks());
                service.storeModel(model);
            }

            PlayerPacket packet = new PlayerPacket();
            packet.setUuid(player.getUuid());
            packet.setValue(player.isMcLeaks());
            PacketUtil.queuePacket(packet);
        });
    }

    public @NotNull CompletableFuture<Void> deletePlayer(@NotNull UUID uniqueId) {
        return CompletableFuture.runAsync(() -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

            for (StorageService service : cachedConfig.getStorage()) {
                PlayerModel model = new PlayerModel();
                model.setUuid(uniqueId);

                service.deleteModel(model);
            }

            DeletePlayerPacket packet = new DeletePlayerPacket();
            packet.setUuid(uniqueId);
            PacketUtil.queuePacket(packet);
        });
    }

    public @NotNull CompletableFuture<@NotNull Set<@NotNull UUID>> getPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

            Set<UUID> retVal = new HashSet<>();
            for (StorageService service : cachedConfig.getStorage()) {
                Set<PlayerModel> models = service.getAllPlayers(cachedConfig.getSourceCacheTime());
                if (!models.isEmpty()) {
                    for (PlayerModel model : models) {
                        retVal.add(model.getUuid());
                    }
                    break;
                }
            }
            return retVal;
        });
    }

    public @NotNull CompletableFuture<@NotNull Boolean> checkMcLeaks(@NotNull UUID uniqueId, boolean useCache) throws APIException {
        return CompletableFuture.supplyAsync(() -> {
            PlayerModel model;
            if (useCache) {
                try {
                    model = playerCache.get(uniqueId);
                } catch (CompletionException ex) {
                    if (ex.getCause() instanceof APIException) {
                        throw (APIException) ex.getCause();
                    } else {
                        throw new APIException(false, "Could not get data for player " + uniqueId, ex);
                    }
                } catch (RuntimeException | Error ex) {
                    throw new APIException(false, "Could not get data for player " + uniqueId, ex);
                }
            } else {
                model = calculatePlayerResult(uniqueId, false);
            }
            if (model == null) {
                throw new APIException(false, "Could not get data for player " + uniqueId);
            }
            return model.isMcleaks();
        });
    }

    private @NotNull PlayerModel calculatePlayerResult(@NotNull UUID uuid, boolean useCache) throws APIException {
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

        try {
            HttpURLConnection conn = WebRequest.builder(new URL("https://mcleaks.themrgong.xyz/api/v3/isuuidmcleaks/" + uuid))
                    .timeout(new TimeUtil.Time(2500L, TimeUnit.MILLISECONDS))
                    .userAgent("egg82/Anti-VPN")
                    .header("API-Key", mcleaksKey)
                    .build()
                    .getConnection();

            JSONDeserializer<MCLeaksResultModel> modelDeserializer = new JSONDeserializer<>();
            MCLeaksResultModel model = modelDeserializer.deserialize(WebRequest.getString(conn));

            if (model.getError() != null) {
                throw new APIException(model.getError().contains("rate limit"), model.getError());
            }
            retVal.setMcleaks(model.isMcLeaks());
        } catch (IOException ex) {
            throw new APIException(false, ex);
        }

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

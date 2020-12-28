package me.egg82.antivpn.api.model.player;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.messaging.packets.DeletePlayerPacket;
import me.egg82.antivpn.messaging.packets.PlayerPacket;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.storage.models.PlayerModel;
import me.egg82.antivpn.utils.PacketUtil;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractPlayerManager implements PlayerManager {
    private final LoadingCache<UUID, PlayerModel> playerCache;

    protected AbstractPlayerManager(long cacheTime, TimeUnit cacheTimeUnit) {
        playerCache = Caffeine.newBuilder().expireAfterAccess(cacheTime, cacheTimeUnit).expireAfterWrite(cacheTime, cacheTimeUnit).build(k -> calculatePlayerResult(k, true));
    }

    public LoadingCache<UUID, PlayerModel> getPlayerCache() { return playerCache; }

    public @NonNull CompletableFuture<Void> savePlayer(@NonNull Player player) {
        return CompletableFuture.runAsync(() -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (cachedConfig == null) {
                throw new APIException(false, "Cached config could not be fetched.");
            }

            for (StorageService service : cachedConfig.getStorage()) {
                PlayerModel model = new PlayerModel();
                model.setUuid(player.getUuid());
                model.setMcleaks(player.isMcLeaks());
                service.storeModel(model);
            }

            PlayerPacket packet = new PlayerPacket();
            packet.setUuid(player.getUuid());
            packet.setValue(player.isMcLeaks());
            PacketUtil.queuePacket(packet);
        });
    }

    public @NonNull CompletableFuture<Void> deletePlayer(@NonNull UUID uniqueId) {
        return CompletableFuture.runAsync(() -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (cachedConfig == null) {
                throw new APIException(false, "Cached config could not be fetched.");
            }

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

    public @NonNull CompletableFuture<Set<UUID>> getPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (cachedConfig == null) {
                throw new APIException(false, "Cached config could not be fetched.");
            }

            Set<UUID> retVal = new HashSet<>();
            for (StorageService service : cachedConfig.getStorage()) {
                Set<PlayerModel> models = service.getAllPlayers(cachedConfig.getSourceCacheTime());
                if (models != null && !models.isEmpty()) {
                    for (PlayerModel model : models) {
                        retVal.add(model.getUuid());
                    }
                    break;
                }
            }
            return retVal;
        });
    }

    public @NonNull CompletableFuture<Boolean> checkMcLeaks(@NonNull UUID uniqueId, boolean useCache) throws APIException {
        return CompletableFuture.supplyAsync(() -> {
            PlayerModel model;
            if (useCache) {
                model = playerCache.get(uniqueId);
            } else {
                model = calculatePlayerResult(uniqueId, false);
            }
            if (model == null) {
                throw new APIException(false, "Could not get data for player " + uniqueId);
            }
            return model.isMcleaks();
        });
    }

    protected abstract @NonNull PlayerModel calculatePlayerResult(@NonNull UUID uuid, boolean useCache) throws APIException;
}

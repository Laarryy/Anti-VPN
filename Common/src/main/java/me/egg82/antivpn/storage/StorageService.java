package me.egg82.antivpn.storage;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import me.egg82.antivpn.storage.models.BaseModel;
import me.egg82.antivpn.storage.models.DataModel;
import me.egg82.antivpn.storage.models.IPModel;
import me.egg82.antivpn.storage.models.PlayerModel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface StorageService {
    @NonNull String getName();

    void close();
    boolean isClosed();

    void storeModel(@NonNull BaseModel model);
    void storeModels(@NonNull Collection<? extends BaseModel> models);
    void deleteModel(@NonNull BaseModel model);

    /*
    Note: Can be an expensive operation
     */
    @NonNull IPModel getOrCreateIpModel(@NonNull String ip, int type);
    @Nullable IPModel getIpModel(@NonNull String ip, long cacheTimeMillis);
    @Nullable IPModel getIpModel(long ipId, long cacheTimeMillis);
    @NonNull Set<IPModel> getAllIps(long cacheTimeMillis);
    @NonNull Set<IPModel> getAllIps(int start, int max);

    /*
   Note: Can be an expensive operation
    */
    @NonNull PlayerModel getOrCreatePlayerModel(@NonNull UUID player, boolean isMcLeaks);
    @Nullable PlayerModel getPlayerModel(@NonNull UUID player, long cacheTimeMillis);
    @Nullable PlayerModel getPlayerModel(long playerId, long cacheTimeMillis);
    @NonNull Set<PlayerModel> getAllPlayers(long cacheTimeMillis);
    @NonNull Set<PlayerModel> getAllPlayers(int start, int max);

    /*
   Note: Can be an expensive operation
    */
    @NonNull DataModel getOrCreateDataModel(@NonNull String key, String value);
    @Nullable DataModel getDataModel(@NonNull String key);
    @Nullable DataModel getDataModel(long dataId);
}

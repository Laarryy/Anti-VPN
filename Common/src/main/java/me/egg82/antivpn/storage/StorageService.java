package me.egg82.antivpn.storage;

import me.egg82.antivpn.storage.models.BaseModel;
import me.egg82.antivpn.storage.models.DataModel;
import me.egg82.antivpn.storage.models.IPModel;
import me.egg82.antivpn.storage.models.PlayerModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface StorageService {
    @NotNull String getName();

    void close();

    boolean isClosed();

    void storeModel(@NotNull BaseModel model);

    void storeModels(@NotNull Collection<@NotNull ? extends BaseModel> models);

    void deleteModel(@NotNull BaseModel model);

    /*
    Note: Can be an expensive operation
     */
    @NotNull IPModel getOrCreateIpModel(@NotNull String ip, int type);

    @Nullable IPModel getIpModel(@NotNull String ip, long cacheTimeMillis);

    @Nullable IPModel getIpModel(long ipId, long cacheTimeMillis);

    @NotNull Set<@NotNull IPModel> getAllIps(long cacheTimeMillis);

    @NotNull Set<@NotNull IPModel> getAllIps(int start, int max);

    /*
   Note: Can be an expensive operation
    */
    @NotNull PlayerModel getOrCreatePlayerModel(@NotNull UUID player, boolean isMcLeaks);

    @Nullable PlayerModel getPlayerModel(@NotNull UUID player, long cacheTimeMillis);

    @Nullable PlayerModel getPlayerModel(long playerId, long cacheTimeMillis);

    @NotNull Set<@NotNull PlayerModel> getAllPlayers(long cacheTimeMillis);

    @NotNull Set<@NotNull PlayerModel> getAllPlayers(int start, int max);

    /*
   Note: Can be an expensive operation
    */
    @NotNull DataModel getOrCreateDataModel(@NotNull String key, String value);

    @Nullable DataModel getDataModel(@NotNull String key);

    @Nullable DataModel getDataModel(long dataId);
}

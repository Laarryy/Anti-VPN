package me.egg82.antivpn.storage;

import java.util.Set;
import java.util.UUID;
import me.egg82.antivpn.storage.models.BaseModel;
import me.egg82.antivpn.storage.models.IPModel;
import me.egg82.antivpn.storage.models.PlayerModel;

public interface StorageService {
    String getName();

    void close();
    boolean isClosed();

    void storeModel(BaseModel model);
    void deleteModel(BaseModel model);

    /*
    Note: Can be an expensive operation
     */
    IPModel getIpModel(String ip, long cacheTimeMillis);
    IPModel getIpModel(long ipId, long cacheTimeMillis);
    Set<IPModel> getAllIps(long cacheTimeMillis);

    /*
   Note: Can be an expensive operation
    */
    PlayerModel getPlayerModel(UUID player, long cacheTimeMillis);
    PlayerModel getPlayerModel(long playerId, long cacheTimeMillis);
    Set<PlayerModel> getAllPlayers(long cacheTimeMillis);
}

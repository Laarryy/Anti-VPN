package me.egg82.antivpn.services;

import java.util.UUID;
import me.egg82.antivpn.storage.Storage;

public interface StorageHandler {
    void ipIDCreationCallback(String ip, long longIPID, Storage callingStorage);
    void playerIDCreationCallback(UUID playerID, long longPlayerID, Storage callingStorage);
}

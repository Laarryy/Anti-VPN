package me.egg82.antivpn.services;

import me.egg82.antivpn.storage.Storage;

import java.util.UUID;

public interface StorageHandler {
    void ipIDCreationCallback(String ip, long longIPID, Storage callingStorage);
    void playerIDCreationCallback(UUID playerID, long longPlayerID, Storage callingStorage);
}

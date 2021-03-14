package me.egg82.antivpn.services.lookup;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class BukkitPlayerInfo extends MojangPlayerInfo {
    BukkitPlayerInfo(@NotNull UUID uuid) throws IOException {
        super(uuid);
    }

    BukkitPlayerInfo(@NotNull String name) throws IOException {
        super(name);
    }

    protected @Nullable String nameExpensive(@NotNull UUID uuid) throws IOException {
        // Currently-online lookup
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            synchronized (nameCacheLock) {
                nameCache.put(player.getName(), uuid);
            }
            return player.getName();
        }

        return super.nameExpensive(uuid);
    }

    protected @Nullable UUID uuidExpensive(@NotNull String name) throws IOException {
        // Currently-online lookup
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            synchronized (uuidCacheLock) {
                uuidCache.put(player.getUniqueId(), name);
            }
            return player.getUniqueId();
        }

        return super.uuidExpensive(name);
    }
}

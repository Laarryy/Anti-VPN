package me.egg82.antivpn.services.lookup;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PaperPlayerInfo implements PlayerInfo {
    private UUID uuid;
    private String name;

    private static Cache<UUID, String> uuidCache = Caffeine.newBuilder().expireAfterAccess(1L, TimeUnit.MINUTES).expireAfterWrite(1L, TimeUnit.HOURS).build();
    private static Cache<String, UUID> nameCache = Caffeine.newBuilder().expireAfterAccess(1L, TimeUnit.MINUTES).expireAfterWrite(1L, TimeUnit.HOURS).build();

    private static final Object uuidCacheLock = new Object();
    private static final Object nameCacheLock = new Object();

    PaperPlayerInfo(UUID uuid) throws IOException {
        this.uuid = uuid;

        Optional<String> name = Optional.ofNullable(uuidCache.getIfPresent(uuid));
        if (!name.isPresent()) {
            synchronized (uuidCacheLock) {
                name = Optional.ofNullable(uuidCache.getIfPresent(uuid));
                if (!name.isPresent()) {
                    name = Optional.ofNullable(nameExpensive(uuid));
                    name.ifPresent(v -> uuidCache.put(uuid, v));
                }
            }
        }

        this.name = name.orElse(null);
    }

    PaperPlayerInfo(String name) throws IOException {
        this.name = name;

        Optional<UUID> uuid = Optional.ofNullable(nameCache.getIfPresent(name));
        if (!uuid.isPresent()) {
            synchronized (nameCacheLock) {
                uuid = Optional.ofNullable(nameCache.getIfPresent(name));
                if (!uuid.isPresent()) {
                    uuid = Optional.ofNullable(uuidExpensive(name));
                    uuid.ifPresent(v -> nameCache.put(name, v));
                }
            }
        }

        this.uuid = uuid.orElse(null);
    }

    public UUID getUUID() { return uuid; }

    public String getName() { return name; }

    private static String nameExpensive(UUID uuid) throws IOException {
        // Currently-online lookup
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            nameCache.put(player.getName(), uuid);
            return player.getName();
        }

        // Cached profile lookup
        PlayerProfile profile = Bukkit.createProfile(uuid);
        if ((profile.isComplete() || profile.completeFromCache()) && profile.getName() != null && profile.getId() != null) {
            nameCache.put(profile.getName(), profile.getId());
            return profile.getName();
        }

        // Network lookup
        if (profile.complete(false) && profile.getName() != null && profile.getId() != null) {
            nameCache.put(profile.getName(), profile.getId());
            return profile.getName();
        }

        // Sorry, nada
        throw new IOException("Could not load player data from Mojang (rate-limited?)");
    }

    private static UUID uuidExpensive(String name) throws IOException {
        // Currently-online lookup
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            uuidCache.put(player.getUniqueId(), name);
            return player.getUniqueId();
        }

        // Cached profile lookup
        PlayerProfile profile = Bukkit.createProfile(name);
        if ((profile.isComplete() || profile.completeFromCache()) && profile.getName() != null && profile.getId() != null) {
            uuidCache.put(profile.getId(), profile.getName());
            return profile.getId();
        }

        // Network lookup
        if (profile.complete(false) && profile.getName() != null && profile.getId() != null) {
            uuidCache.put(profile.getId(), profile.getName());
            return profile.getId();
        }

        // Sorry, nada
        throw new IOException("Could not load player data from Mojang (rate-limited?)");
    }
}

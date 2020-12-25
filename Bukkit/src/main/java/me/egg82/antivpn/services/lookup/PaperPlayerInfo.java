package me.egg82.antivpn.services.lookup;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.services.lookup.models.ProfileModel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PaperPlayerInfo implements PlayerInfo {
    private final UUID uuid;
    private final String name;
    private List<ProfileModel.ProfilePropertyModel> properties;

    private static final Cache<UUID, String> uuidCache = Caffeine.newBuilder().expireAfterAccess(1L, TimeUnit.MINUTES).expireAfterWrite(1L, TimeUnit.HOURS).build();
    private static final Cache<String, UUID> nameCache = Caffeine.newBuilder().expireAfterAccess(1L, TimeUnit.MINUTES).expireAfterWrite(1L, TimeUnit.HOURS).build();
    private static final Cache<UUID, List<ProfileModel.ProfilePropertyModel>> propertiesCache = Caffeine.newBuilder().expireAfterAccess(1L, TimeUnit.MINUTES).expireAfterWrite(1L, TimeUnit.DAYS).build();

    private static final Object uuidCacheLock = new Object();
    private static final Object nameCacheLock = new Object();
    private static final Object propertiesCacheLock = new Object();

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

        if (this.name != null) {
            Optional<List<ProfileModel.ProfilePropertyModel>> properties = Optional.ofNullable(propertiesCache.getIfPresent(uuid));
            if (!properties.isPresent()) {
                synchronized (propertiesCacheLock) {
                    properties = Optional.ofNullable(propertiesCache.getIfPresent(uuid));
                    if (!properties.isPresent()) {
                        properties = Optional.ofNullable(propertiesExpensive(uuid));
                        properties.ifPresent(v -> propertiesCache.put(uuid, v));
                    }
                }
            }
            this.properties = properties.orElse(null);
        }
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

        if (this.uuid != null) {
            Optional<List<ProfileModel.ProfilePropertyModel>> properties = Optional.ofNullable(propertiesCache.getIfPresent(this.uuid));
            if (!properties.isPresent()) {
                synchronized (propertiesCacheLock) {
                    properties = Optional.ofNullable(propertiesCache.getIfPresent(this.uuid));
                    if (!properties.isPresent()) {
                        properties = Optional.ofNullable(propertiesExpensive(this.uuid));
                        properties.ifPresent(v -> propertiesCache.put(this.uuid, v));
                    }
                }
            }
            this.properties = properties.orElse(null);
        }
    }

    public @NonNull UUID getUUID() { return uuid; }

    public @NonNull String getName() { return name; }

    public @NonNull ImmutableList<ProfileModel.ProfilePropertyModel> getProperties() { return ImmutableList.copyOf(properties); }

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

    private static List<ProfileModel.ProfilePropertyModel> propertiesExpensive(UUID uuid) throws IOException {
        // Cached profile lookup
        PlayerProfile profile = Bukkit.createProfile(uuid);
        if ((profile.isComplete() || profile.completeFromCache()) && profile.getName() != null && profile.getId() != null) {
            return toPropertiesModel(profile.getProperties());
        }

        // Network lookup
        if (profile.complete(false) && profile.getName() != null && profile.getId() != null) {
            return toPropertiesModel(profile.getProperties());
        }

        throw new IOException("Could not load skin data from Mojang (rate-limited?)");
    }

    private static List<ProfileModel.ProfilePropertyModel> toPropertiesModel(Set<ProfileProperty> properties) {
        List<ProfileModel.ProfilePropertyModel> retVal = new ArrayList<>();
        for (ProfileProperty property : properties) {
            ProfileModel.ProfilePropertyModel newProperty = new ProfileModel.ProfilePropertyModel();
            newProperty.setName(property.getName());
            newProperty.setValue(property.getValue());
            newProperty.setSignature(property.getSignature());
            retVal.add(newProperty);
        }
        return retVal;
    }
}

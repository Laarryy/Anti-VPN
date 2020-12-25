package me.egg82.antivpn.services.lookup;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableList;
import flexjson.JSONDeserializer;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.services.lookup.models.PlayerNameModel;
import me.egg82.antivpn.services.lookup.models.PlayerUUIDModel;
import me.egg82.antivpn.services.lookup.models.ProfileModel;
import me.egg82.antivpn.utils.WebUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BukkitPlayerInfo implements PlayerInfo {
    private final UUID uuid;
    private final String name;
    private List<ProfileModel.ProfilePropertyModel> properties;

    private static final Cache<UUID, String> uuidCache = Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.HOURS).build();
    private static final Cache<String, UUID> nameCache = Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.HOURS).build();
    private static final Cache<UUID, List<ProfileModel.ProfilePropertyModel>> propertiesCache = Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.DAYS).build();

    private static final Object uuidCacheLock = new Object();
    private static final Object nameCacheLock = new Object();
    private static final Object propertiesCacheLock = new Object();

    private static final Map<String, String> headers = new HashMap<>();
    static {
        headers.put("Accept", "application/json");
        headers.put("Connection", "close");
        headers.put("Accept-Language", "en-US,en;q=0.8");
    }

    BukkitPlayerInfo(UUID uuid) throws IOException {
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

    BukkitPlayerInfo(String name) throws IOException {
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
            synchronized (nameCacheLock) {
                nameCache.put(player.getName(), uuid);
            }
            return player.getName();
        }

        // Network lookup
        HttpURLConnection conn = WebUtil.getConnection(new URL("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names"), "GET", 5000, "egg82/PlayerInfo", headers);
        int status = conn.getResponseCode();

        if (status == 204) {
            // No data exists
            return null;
        } else if (status == 200) {
            JSONDeserializer<List<PlayerNameModel>> modelDeserializer = new JSONDeserializer<>();
            modelDeserializer.use("values", PlayerNameModel.class);
            List<PlayerNameModel> model = modelDeserializer.deserialize(WebUtil.getString(conn));

            String name = model.get(model.size() - 1).getName();
            synchronized (nameCacheLock) {
                nameCache.put(name, uuid);
            }
            return name;
        }

        throw new IOException("Could not load player data from Mojang (rate-limited?)");
    }

    private static UUID uuidExpensive(String name) throws IOException {
        // Currently-online lookup
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            synchronized (uuidCacheLock) {
                uuidCache.put(player.getUniqueId(), name);
            }
            return player.getUniqueId();
        }

        // Network lookup
        HttpURLConnection conn = WebUtil.getConnection(new URL("https://api.mojang.com/users/profiles/minecraft/" + WebUtil.urlEncode(name)), "GET", 5000, "egg82/PlayerInfo", headers);
        int status = conn.getResponseCode();

        if (status == 204) {
            // No data exists
            return null;
        } else if (status == 200) {
            JSONDeserializer<PlayerUUIDModel> modelDeserializer = new JSONDeserializer<>();
            PlayerUUIDModel model = modelDeserializer.deserialize(WebUtil.getString(conn), PlayerUUIDModel.class);

            UUID uuid = UUID.fromString(model.getId().replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
            synchronized (uuidCacheLock) {
                uuidCache.put(uuid, name);
            }
            return uuid;
        }

        throw new IOException("Could not load player data from Mojang (rate-limited?)");
    }

    private static List<ProfileModel.ProfilePropertyModel> propertiesExpensive(UUID uuid) throws IOException {
        // Network lookup
        HttpURLConnection conn = WebUtil.getConnection(new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false"), "GET", 5000, "egg82/PlayerInfo", headers);
        int status = conn.getResponseCode();

        if (status == 204) {
            // No data exists
            return null;
        } else if (status == 200) {
            JSONDeserializer<ProfileModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(WebUtil.getString(conn), ProfileModel.class).getProperties();
        }

        throw new IOException("Could not load skin data from Mojang (rate-limited?)");
    }
}

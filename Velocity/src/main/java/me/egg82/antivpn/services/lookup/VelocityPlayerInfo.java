package me.egg82.antivpn.services.lookup;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import flexjson.JSONDeserializer;
import me.egg82.antivpn.services.lookup.models.PlayerNameModel;
import me.egg82.antivpn.services.lookup.models.PlayerUUIDModel;
import me.egg82.antivpn.services.lookup.models.ProfileModel;
import me.egg82.antivpn.utils.TimeUtil;
import me.egg82.antivpn.web.WebRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VelocityPlayerInfo implements PlayerInfo {
    private final UUID uuid;
    private final String name;
    private List<ProfileModel.ProfilePropertyModel> properties;

    private static final Cache<UUID, String> uuidCache = Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.HOURS).build();
    private static final Cache<String, UUID> nameCache = Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.HOURS).build();
    private static final Cache<UUID, List<ProfileModel.ProfilePropertyModel>> propertiesCache = Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.DAYS).build();

    private static final Object uuidCacheLock = new Object();
    private static final Object nameCacheLock = new Object();
    private static final Object propertiesCacheLock = new Object();

    VelocityPlayerInfo(@NotNull UUID uuid, @NotNull ProxyServer proxy) throws IOException {
        this.uuid = uuid;

        Optional<String> name = Optional.ofNullable(uuidCache.getIfPresent(uuid));
        if (!name.isPresent()) {
            synchronized (uuidCacheLock) {
                name = Optional.ofNullable(uuidCache.getIfPresent(uuid));
                if (!name.isPresent()) {
                    name = Optional.ofNullable(nameExpensive(uuid, proxy));
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

    VelocityPlayerInfo(@NotNull String name, @NotNull ProxyServer proxy) throws IOException {
        this.name = name;

        Optional<UUID> uuid = Optional.ofNullable(nameCache.getIfPresent(name));
        if (!uuid.isPresent()) {
            synchronized (nameCacheLock) {
                uuid = Optional.ofNullable(nameCache.getIfPresent(name));
                if (!uuid.isPresent()) {
                    uuid = Optional.ofNullable(uuidExpensive(name, proxy));
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

    @Override
    public @NotNull UUID getUUID() { return uuid; }

    @Override
    public @NotNull String getName() { return name; }

    @Override
    public @NotNull ImmutableList<ProfileModel.ProfilePropertyModel> getProperties() { return ImmutableList.copyOf(properties); }

    private static @Nullable String nameExpensive(@NotNull UUID uuid, @NotNull ProxyServer proxy) throws IOException {
        // Currently-online lookup
        Optional<Player> player = proxy.getPlayer(uuid);
        if (player.isPresent()) {
            synchronized (nameCacheLock) {
                nameCache.put(player.get().getUsername(), uuid);
            }
            return player.get().getUsername();
        }

        // Network lookup
        HttpURLConnection conn = WebRequest.builder(new URL("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names"))
                .timeout(new TimeUtil.Time(2500L, TimeUnit.MILLISECONDS))
                .userAgent("egg82/PlayerInfo")
                .header("Accept", "application/json")
                .build()
                .getConnection();
        int status = conn.getResponseCode();

        if (status == 204) {
            // No data exists
            return null;
        } else if (status == 200) {
            JSONDeserializer<List<PlayerNameModel>> modelDeserializer = new JSONDeserializer<>();
            modelDeserializer.use("values", PlayerNameModel.class);
            List<PlayerNameModel> model = modelDeserializer.deserialize(WebRequest.getString(conn));

            String name = model.get(model.size() - 1).getName();
            synchronized (nameCacheLock) {
                nameCache.put(name, uuid);
            }
            return name;
        }

        throw new IOException("Mojang API response code: " + status);
    }

    private static @Nullable UUID uuidExpensive(@NotNull String name, @NotNull ProxyServer proxy) throws IOException {
        // Currently-online lookup
        Optional<Player> player = proxy.getPlayer(name);
        if (player.isPresent()) {
            synchronized (uuidCacheLock) {
                uuidCache.put(player.get().getUniqueId(), name);
            }
            return player.get().getUniqueId();
        }

        // Network lookup
        HttpURLConnection conn = WebRequest.builder(new URL("https://api.mojang.com/users/profiles/minecraft/" + WebRequest.urlEncode(name)))
                .timeout(new TimeUtil.Time(2500L, TimeUnit.MILLISECONDS))
                .userAgent("egg82/PlayerInfo")
                .header("Accept", "application/json")
                .build()
                .getConnection();
        int status = conn.getResponseCode();

        if (status == 204) {
            // No data exists
            return null;
        } else if (status == 200) {
            JSONDeserializer<PlayerUUIDModel> modelDeserializer = new JSONDeserializer<>();
            PlayerUUIDModel model = modelDeserializer.deserialize(WebRequest.getString(conn), PlayerUUIDModel.class);

            UUID uuid = UUID.fromString(model.getId().replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
            synchronized (uuidCacheLock) {
                uuidCache.put(uuid, name);
            }
            return uuid;
        }

        throw new IOException("Mojang API response code: " + status);
    }

    private static @Nullable List<ProfileModel.ProfilePropertyModel> propertiesExpensive(@NotNull UUID uuid) throws IOException {
        // Network lookup
        HttpURLConnection conn = WebRequest.builder(new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString()
                .replace("-", "") + "?unsigned=false"))
                .timeout(new TimeUtil.Time(2500L, TimeUnit.MILLISECONDS))
                .userAgent("egg82/PlayerInfo")
                .header("Accept", "application/json")
                .build()
                .getConnection();
        int status = conn.getResponseCode();

        if (status == 204) {
            // No data exists
            return null;
        } else if (status == 200) {
            JSONDeserializer<ProfileModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(WebRequest.getString(conn), ProfileModel.class).getProperties();
        }

        throw new IOException("Mojang API response code: " + status);
    }
}

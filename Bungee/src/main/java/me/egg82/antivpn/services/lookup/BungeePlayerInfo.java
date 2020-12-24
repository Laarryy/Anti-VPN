package me.egg82.antivpn.services.lookup;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ninja.egg82.json.JSONUtil;
import ninja.egg82.json.JSONWebUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class BungeePlayerInfo implements PlayerInfo {
    private UUID uuid;
    private String name;

    private static Cache<UUID, String> uuidCache = Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.HOURS).build();
    private static Cache<String, UUID> nameCache = Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.HOURS).build();

    private static final Object uuidCacheLock = new Object();
    private static final Object nameCacheLock = new Object();

    private static final Map<String, String> headers = new HashMap<>();

    static {
        headers.put("Accept", "application/json");
        headers.put("Connection", "close");
        headers.put("Accept-Language", "en-US,en;q=0.8");
    }

    BungeePlayerInfo(UUID uuid) throws IOException {
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

    BungeePlayerInfo(String name) throws IOException {
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
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player != null) {
            nameCache.put(player.getName(), uuid);
            return player.getName();
        }

        // Network lookup
        HttpURLConnection conn = JSONWebUtil.getConnection(new URL("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names"), "GET", 5000, "egg82/PlayerInfo", headers);;
        int status = conn.getResponseCode();

        if (status == 204) {
            // No data exists
            return null;
        } else if (status == 200) {
            try {
                JSONArray json = getJSONArray(conn, status);
                JSONObject last = (JSONObject) json.get(json.size() - 1);
                String name = (String) last.get("name");
                nameCache.put(name, uuid);
                return name;
            } catch (ParseException | ClassCastException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }

        throw new IOException("Could not load player data from Mojang (rate-limited?)");
    }

    private static UUID uuidExpensive(String name) throws IOException {
        // Currently-online lookup
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(name);
        if (player != null) {
            uuidCache.put(player.getUniqueId(), name);
            return player.getUniqueId();
        }

        // Network lookup
        HttpURLConnection conn = JSONWebUtil.getConnection(new URL("https://api.mojang.com/users/profiles/minecraft/" + name), "GET", 5000, "egg82/PlayerInfo", headers);
        int status = conn.getResponseCode();

        if (status == 204) {
            // No data exists
            return null;
        } else if (status == 200) {
            try {
                JSONObject json = getJSONObject(conn, status);
                UUID uuid = UUID.fromString(((String) json.get("id")).replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
                name = (String) json.get("name");
                uuidCache.put(uuid, name);
                return uuid;
            } catch (ParseException | ClassCastException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }

        throw new IOException("Could not load player data from Mojang (rate-limited?)");
    }

    public static JSONArray getJSONArray(HttpURLConnection conn, int status) throws IOException, ParseException, ClassCastException {
        return JSONUtil.parseArray(getString(conn, status));
    }

    private static JSONObject getJSONObject(HttpURLConnection conn, int status) throws IOException, ParseException, ClassCastException {
        return JSONUtil.parseObject(getString(conn, status));
    }

    private static String getString(HttpURLConnection conn, int status) throws IOException {
        try (InputStream in = getInputStream(conn, status); InputStreamReader reader = new InputStreamReader(in); BufferedReader buffer = new BufferedReader(reader)) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = buffer.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    private static InputStream getInputStream(HttpURLConnection conn, int status) throws IOException {
        if (status >= 400 && status < 600) {
            // 400-500 errors
            throw new IOException("Server returned status code " + status);
        }

        return conn.getInputStream();
    }
}

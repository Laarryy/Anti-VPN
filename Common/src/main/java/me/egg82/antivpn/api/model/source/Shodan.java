package me.egg82.antivpn.api.model.source;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class Shodan extends AbstractSource {
    public @NonNull String getName() { return "shodan"; }

    public boolean isKeyRequired() { return true; }

    public @NonNull CompletableFuture<Boolean> getResult(@NonNull String ip) {
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        ConfigurationNode sourceConfigNode = getSourceConfigNode();

        String key = sourceConfigNode.getNode("key").getString();
        if (key == null || key.isEmpty()) {
            throw new APIException(true, "Key is not defined for " + getName());
        }

        JSONObject json;
        try {
            json = JSONWebUtil.getJSONObject(new URL("https://api.shodan.io/shodan/host/" + ip + "?key=" + key), "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN");
        } catch (IOException | ParseException | ClassCastException ex) {
            throw new APIException(false, "Could not get result from " + getName());
        }
        if (json == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        JSONArray tags = (JSONArray) json.get("tags");
        if (tags == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        if (tags.isEmpty()) {
            return false;
        }
        for (Object tag : tags) {
            String t = (String) tag;
            if (t.equalsIgnoreCase("proxy") || t.equalsIgnoreCase("vpn")) {
                return true;
            }
        }

        return false;
    }
}

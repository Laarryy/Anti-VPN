package me.egg82.antivpn.api.model.source;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class IPWarner extends AbstractSource {
    public @NonNull String getName() { return "ipwarner"; }

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
            json = JSONWebUtil.getJSONObject(new URL("https://api.ipwarner.com/" + key + "/" + ip), "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN");
        } catch (IOException | ParseException | ClassCastException ignored) {
            try {
                json = JSONWebUtil.getJSONObject(new URL("http://api.ipwarner.com/" + key + "/" + ip), "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN"); // Temporary (hopefully) hack
            } catch (IOException | ParseException | ClassCastException ex) {
                throw new APIException(false, ex);
            }
        }
        if (json == null || json.get("goodIp") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        short retVal = ((Number) json.get("goodIp")).shortValue();
        if (retVal < 0) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        return retVal == 1;
    }
}

package me.egg82.antivpn.apis.vpn;

import me.egg82.antivpn.APIException;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URL;

public class Shodan extends AbstractSourceAPI {
    public String getName() { return "shodan"; }

    public boolean isKeyRequired() { return true; }

    public boolean getResult(String ip) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
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

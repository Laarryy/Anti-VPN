package me.egg82.antivpn.apis;

import java.io.IOException;
import java.util.Optional;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShodanAPI implements API {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "shodan"; }

    public Optional<Boolean> getResult(String ip, ConfigurationNode sourceConfigNode) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (sourceConfigNode == null) {
            throw new IllegalArgumentException("sourceConfigNode cannot be null.");
        }

        String key = sourceConfigNode.getNode("key").getString();
        if (key == null || key.isEmpty()) {
            return Optional.empty();
        }

        JSONObject json;
        try {
            json = JSONWebUtil.getJsonObject("https://api.shodan.io/shodan/host/" + ip + "?key=" + key, "egg82/AntiVPN");
        } catch (IOException | ParseException ex) {
            logger.error(ex.getMessage(), ex);
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }

        JSONArray tags = (JSONArray) json.get("tags");
        if (tags == null) {
            return Optional.empty();
        }

        if (tags.isEmpty()) {
            return Optional.of(Boolean.FALSE);
        }
        for (Object tag : tags) {
            String t = (String) tag;
            if (t.equalsIgnoreCase("vpn")) {
                return Optional.of(Boolean.TRUE);
            }
        }

        return Optional.of(Boolean.FALSE);
    }
}

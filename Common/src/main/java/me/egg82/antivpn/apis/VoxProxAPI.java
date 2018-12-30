package me.egg82.antivpn.apis;

import java.io.IOException;
import java.util.Optional;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoxProxAPI implements API {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "voxprox"; }

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
            json = JSONWebUtil.getJsonObject("https://www.voxprox.com/pmx.php?key=" + key + "&ip=" + ip, "egg82/AntiVPN");
        } catch (IOException | ParseException ex) {
            logger.error(ex.getMessage(), ex);
            return Optional.empty();
        }
        if (json == null || json.get("Result") == null) {
            return Optional.empty();
        }

        return Optional.of((((String) json.get("Result")).equalsIgnoreCase("true")) ? Boolean.TRUE : Boolean.FALSE);
    }
}

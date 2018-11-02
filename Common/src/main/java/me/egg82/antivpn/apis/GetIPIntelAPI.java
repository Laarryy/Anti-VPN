package me.egg82.antivpn.apis;

import java.io.IOException;
import java.util.Optional;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetIPIntelAPI implements API {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "getipintel"; }

    public Optional<Boolean> getResult(String ip, ConfigurationNode sourceConfigNode) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (sourceConfigNode == null) {
            throw new IllegalArgumentException("sourceConfigNode cannot be null.");
        }

        JSONObject json;
        try {
            json = JSONWebUtil.getJsonObject("https://check.getipintel.net/check.php?ip=" + ip + "&contact=" + sourceConfigNode.getNode("contact").getString("") + "&format=json&flags=b", "egg82/AntiVPN");
        } catch (IOException | ParseException ex) {
            logger.error(ex.getMessage(), ex);
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }

        double retVal = Double.parseDouble((String) json.get("result"));
        if (retVal < 0.0d) {
            return Optional.empty();
        }

        return Optional.of((retVal >= sourceConfigNode.getNode("threshold").getDouble()) ? Boolean.TRUE : Boolean.FALSE);
    }
}

package me.egg82.antivpn.apis;

import java.io.IOException;
import java.util.Optional;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPQualityScoreAPI implements API {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "ipqualityscore"; }

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
            json = JSONWebUtil.getJsonObject("http://www.ipqualityscore.com/api/json/ip/" + key + "/" + ip, "egg82/AntiVPN");
        } catch (IOException | ParseException ex) {
            logger.error(ex.getMessage(), ex);
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }

        if (!((Boolean) json.get("success"))) {
            return Optional.empty();
        }

        if (((Boolean) json.get("proxy"))) {
            return Optional.of(Boolean.TRUE);
        }
        if (((Boolean) json.get("vpn"))) {
            return Optional.of(Boolean.TRUE);
        }
        if (((Boolean) json.get("tor"))) {
            return Optional.of(Boolean.TRUE);
        }
        if (((Boolean) json.get("is_crawler"))) {
            return Optional.of(Boolean.TRUE);
        }

        double retVal = ((Number) json.get("fraud_score")).doubleValue();
        if (retVal < 0.0d) {
            return Optional.empty();
        }

        return Optional.of((retVal >= sourceConfigNode.getNode("threshold").getDouble()) ? Boolean.TRUE : Boolean.FALSE);
    }
}

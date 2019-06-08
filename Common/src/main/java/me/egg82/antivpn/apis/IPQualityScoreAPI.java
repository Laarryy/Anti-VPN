package me.egg82.antivpn.apis;

import java.io.IOException;
import java.util.Optional;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.utils.ConfigUtil;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPQualityScoreAPI implements API {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "ipqualityscore"; }

    public boolean getResult(String ip) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }

        ConfigurationNode sourceConfigNode = getSourceConfigNode();

        String key = sourceConfigNode.getNode("key").getString();
        if (key == null || key.isEmpty()) {
            throw new APIException(true, "Key is not defined for " + getName());
        }

        JSONObject json;
        try {
            json = JSONWebUtil.getJsonObject("http://www.ipqualityscore.com/api/json/ip/" + key + "/" + ip, "egg82/AntiVPN");
        } catch (IOException | ParseException ex) {
            logger.error(ex.getMessage(), ex);
            throw new APIException(false, "Could not get result from " + getName());
        }
        if (json == null || json.get("success") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        if (!((Boolean) json.get("success"))) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        if (json.get("proxy") != null && ((Boolean) json.get("proxy"))) {
            return true;
        }
        if (json.get("vpn") != null && ((Boolean) json.get("vpn"))) {
            return true;
        }
        if (json.get("tor") != null && ((Boolean) json.get("tor"))) {
            return true;
        }
        if (json.get("is_crawler") != null && ((Boolean) json.get("is_crawler"))) {
            return true;
        }

        if (json.get("fraud_score") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        double retVal = ((Number) json.get("fraud_score")).doubleValue();
        if (retVal < 0.0d) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        return retVal >= sourceConfigNode.getNode("threshold").getDouble();
    }

    private ConfigurationNode getSourceConfigNode() throws APIException {
        Optional<Configuration> config = ConfigUtil.getConfig();
        if (!config.isPresent()) {
            throw new APIException(true, "Could not get configuration.");
        }

        return config.get().getNode("sources", getName());
    }
}

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

public class GetIPIntelAPI implements API {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "getipintel"; }

    public boolean getResult(String ip) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }

        ConfigurationNode sourceConfigNode = getSourceConfigNode();

        JSONObject json;
        try {
            json = JSONWebUtil.getJsonObject("https://check.getipintel.net/check.php?ip=" + ip + "&contact=" + sourceConfigNode.getNode("contact").getString("") + "&format=json&flags=b", "egg82/AntiVPN");
        } catch (IOException | ParseException ex) {
            logger.error(ex.getMessage(), ex);
            throw new APIException(false, ex);
        }
        if (json == null || json.get("result") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        double retVal = Double.parseDouble((String) json.get("result"));
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

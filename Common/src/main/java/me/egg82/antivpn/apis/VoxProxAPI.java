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

public class VoxProxAPI implements API {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "voxprox"; }

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
            json = JSONWebUtil.getJsonObject("https://www.voxprox.com/pmx.php?key=" + key + "&ip=" + ip, "egg82/AntiVPN");
        } catch (IOException | ParseException ex) {
            logger.error(ex.getMessage(), ex);
            throw new APIException(false, "Could not get result from " + getName());
        }
        if (json == null || json.get("Result") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        return ((String) json.get("Result")).equalsIgnoreCase("true");
    }

    private ConfigurationNode getSourceConfigNode() throws APIException {
        Optional<Configuration> config = ConfigUtil.getConfig();
        if (!config.isPresent()) {
            throw new APIException(true, "Could not get configuration.");
        }

        return config.get().getNode("sources", getName());
    }
}

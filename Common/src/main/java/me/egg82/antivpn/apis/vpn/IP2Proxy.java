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

public class IP2ProxyAPI implements API {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "ip2proxy"; }

    public boolean isKeyRequired() { return false; }

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
            json = JSONWebUtil.getJsonObject("https://api.ip2proxy.com/?ip=" + ip + "&key=" + key + "&package=PX1&format=json", "egg82/AntiVPN");
        } catch (IOException | ParseException ex) {
            logger.error(ex.getMessage(), ex);
            throw new APIException(false, "Could not get result from " + getName());
        }
        if (json == null || json.get("response") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        String status = (String) json.get("response");
        if (!status.equalsIgnoreCase("OK")) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        if (json.get("isProxy") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }
        String proxy = (String) json.get("isProxy");

        return proxy.equalsIgnoreCase("YES");
    }

    private ConfigurationNode getSourceConfigNode() throws APIException {
        Optional<Configuration> config = ConfigUtil.getConfig();
        if (!config.isPresent()) {
            throw new APIException(true, "Could not get configuration.");
        }

        return config.get().getNode("sources", getName());
    }
}

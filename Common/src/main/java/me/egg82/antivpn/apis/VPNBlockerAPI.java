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

public class VPNBlockerAPI implements API {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "vpnblocker"; }

    public boolean getResult(String ip) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }

        ConfigurationNode sourceConfigNode = getSourceConfigNode();

        String key = sourceConfigNode.getNode("key").getString();

        JSONObject json;
        try {
            json = JSONWebUtil.getJsonObject("http" + ((key != null && !key.isEmpty()) ? "s" : "") + "://api.vpnblocker.net/v2/json/" + ip + ((key != null && !key.isEmpty()) ? "/" + key : ""), "egg82/AntiVPN");
        } catch (IOException | ParseException ex) {
            logger.error(ex.getMessage(), ex);
            throw new APIException(false, "Could not get result from " + getName());
        }
        if (json == null || json.get("status") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        String status = (String) json.get("status");
        if (!status.equalsIgnoreCase("success")) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        if (json.get("host-ip") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        return (Boolean) json.get("host-ip");
    }

    private ConfigurationNode getSourceConfigNode() throws APIException {
        Optional<Configuration> config = ConfigUtil.getConfig();
        if (!config.isPresent()) {
            throw new APIException(true, "Could not get configuration.");
        }

        return config.get().getNode("sources", getName());
    }
}

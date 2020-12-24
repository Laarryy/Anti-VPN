package me.egg82.antivpn.apis.vpn;

import java.io.IOException;
import java.net.URL;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPInfo extends AbstractSourceAPI {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "ipinfo"; }

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
            json = JSONWebUtil.getJSONObject(new URL("https://ipinfo.io/" + ip + "/privacy?token=" + key), "GET", (int) getCachedConfig().getTimeout());
        } catch (IOException | ParseException | ClassCastException ex) {
            throw new APIException(false, "Could not get result from " + getName());
        }
        if (json == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }
        if (json.isEmpty()) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        // if proxy config setting is true and "proxy" is true, tor || vpn will also be true.
        if (sourceConfigNode.getNode("proxy").getBoolean() && json.get("proxy") != null) {
            if ((Boolean) json.get("proxy")) {
                return true;
            }
        }
        return json.get("vpn") != null && (Boolean) json.get("vpn");
    }
}

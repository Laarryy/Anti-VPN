package me.egg82.antivpn.apis.vpn;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import me.egg82.antivpn.APIException;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPHub extends AbstractVPNAPI {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "iphub"; }

    public boolean isKeyRequired() { return true; }

    public boolean getResult(String ip) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }

        ConfigurationNode sourceConfigNode = getSourceConfigNode();

        String key = sourceConfigNode.getNode("key").getString();
        if (key == null || key.isEmpty()) {
            throw new APIException(true, "Key is not defined for " + getName());
        }

        int blockType = sourceConfigNode.getNode("block").getInt(1);

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Key", key);

        JSONObject json;
        try {
            json = JSONWebUtil.getJSONObject(new URL("https://v2.api.iphub.info/ip/" + ip), "URL", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN", headers);
        } catch (IOException | ParseException | ClassCastException ex) {
            logger.error(ex.getMessage(), ex);
            throw new APIException(false, "Could not get result from " + getName());
        }
        if (json == null || json.get("block") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        int block = ((Number) json.get("block")).intValue();
        return block == blockType;
    }
}

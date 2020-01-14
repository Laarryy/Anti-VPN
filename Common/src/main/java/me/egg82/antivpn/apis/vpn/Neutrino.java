package me.egg82.antivpn.apis.vpn;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Neutrino extends AbstractSourceAPI {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "neutrino"; }

    public boolean isKeyRequired() { return true; }

    public boolean getResult(String ip) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        ConfigurationNode sourceConfigNode = getSourceConfigNode();

        String id = sourceConfigNode.getNode("id").getString();
        if (id == null || id.isEmpty()) {
            throw new APIException(true, "User ID is not defined for " + getName());
        }

        String key = sourceConfigNode.getNode("key").getString();
        if (key == null || key.isEmpty()) {
            throw new APIException(true, "Key is not defined for " + getName());
        }

        Map<String, String> postData = new HashMap<>();
        postData.put("user-id", id);
        postData.put("api-key", key);
        postData.put("output-format", "JSON");
        postData.put("output-case", "kebab");
        postData.put("ip", ip);

        JSONObject json;
        try {
            json = JSONWebUtil.getJSONObject(new URL("https://neutrinoapi.net/ip-probe"), "POST", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN", null, postData);
        } catch (IOException | ParseException | ClassCastException ex) {
            logger.error(ex.getMessage(), ex);
            throw new APIException(false, "Could not get result from " + getName());
        }
        if (json == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        boolean isHosting = (Boolean) json.get("is-hosting");
        boolean isProxy = (Boolean) json.get("is-proxy");
        boolean isVPN = (Boolean) json.get("is-vpn");

        return isHosting || isProxy || isVPN;
    }
}

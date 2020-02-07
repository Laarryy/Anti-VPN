package me.egg82.antivpn.apis.vpn;

import java.io.IOException;
import java.net.URL;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class IP2Proxy extends AbstractSourceAPI {
    public String getName() { return "ip2proxy"; }

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
            json = JSONWebUtil.getJSONObject(new URL("https://api.ip2proxy.com/?ip=" + ip + "&key=" + key + "&package=PX1&format=json"), "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN");
        } catch (IOException | ParseException | ClassCastException ex) {
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
}

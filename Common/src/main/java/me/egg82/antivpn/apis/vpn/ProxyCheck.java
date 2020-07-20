package me.egg82.antivpn.apis.vpn;

import me.egg82.antivpn.APIException;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URL;

public class ProxyCheck extends AbstractSourceAPI {
    public String getName() { return "proxycheck"; }

    public boolean isKeyRequired() { return false; }

    public boolean getResult(String ip) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        ConfigurationNode sourceConfigNode = getSourceConfigNode();

        String key = sourceConfigNode.getNode("key").getString();

        JSONObject json;
        try {
            json = JSONWebUtil.getJSONObject(new URL("https://proxycheck.io/v2/" + ip + "?vpn=1" + ((key != null && !key.isEmpty()) ? "&key=" + key : "")), "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN");
        } catch (IOException | ParseException | ClassCastException ex) {
            throw new APIException(false, "Could not get result from " + getName());
        }
        if (json == null || json.get("status") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        String status = (String) json.get("status");
        if (!status.equalsIgnoreCase("ok")) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        JSONObject result = (JSONObject) json.get(ip);
        if (result == null || result.get("proxy") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }
        String proxy = (String) result.get("proxy");

        return proxy.equalsIgnoreCase("yes");
    }
}

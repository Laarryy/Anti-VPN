package me.egg82.antivpn.apis.vpn;

import java.io.IOException;
import java.net.URL;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class VPNBlocker extends AbstractSourceAPI {
    public String getName() { return "vpnblocker"; }

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
            json = JSONWebUtil.getJSONObject(new URL("http" + ((key != null && !key.isEmpty()) ? "s" : "") + "://api.vpnblocker.net/v2/json/" + ip + ((key != null && !key.isEmpty()) ? "/" + key : "")), "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN");
        } catch (IOException | ParseException | ClassCastException ex) {
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
}

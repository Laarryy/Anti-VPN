package me.egg82.antivpn.apis.vpn;

import java.io.IOException;
import java.net.URL;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.json.JSONWebUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class IPTrooper extends AbstractSourceAPI {
    public String getName() { return "iptrooper"; }

    public boolean isKeyRequired() { return false; }

    public boolean getResult(String ip) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        JSONObject json;
        try {
            json = JSONWebUtil.getJSONObject(new URL("https://api.iptrooper.net/check/" + ip + "?full=1"), "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN");
        } catch (IOException | ParseException | ClassCastException ex) {
            throw new APIException(false, ex);
        }
        if (json == null || json.get("bad") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        return (Boolean) json.get("bad");
    }
}

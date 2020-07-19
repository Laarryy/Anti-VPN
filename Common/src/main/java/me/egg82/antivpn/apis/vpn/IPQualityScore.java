package me.egg82.antivpn.apis.vpn;

import me.egg82.antivpn.APIException;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class IPQualityScore extends AbstractSourceAPI {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "ipqualityscore"; }

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

        int strictness = sourceConfigNode.getNode("strictness").getInt();
        if (strictness < 0) {
            logger.warn("strictness capped at a min of 0");
            strictness = 0;
        }
        if (strictness > 3) {
            logger.warn("strictness capped at a max of 3");
            strictness = 3;
        }

        JSONObject json;
        try {
            json = JSONWebUtil.getJSONObject(new URL("http://www.ipqualityscore.com/api/json/ip/" + key + "/" + ip + "?strictness=" + strictness + "&mobile=" + (sourceConfigNode.getNode("mobile").getBoolean() ? "true" : "false") + "&fast=true&allow_public_access_points=true&lighter_penalties=true"), "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN");
        } catch (IOException | ParseException | ClassCastException ex) {
            throw new APIException(false, "Could not get result from " + getName());
        }
        if (json == null || json.get("success") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        if (!((Boolean) json.get("success"))) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        if (sourceConfigNode.getNode("proxy").getBoolean() && json.get("proxy") != null) {
            if (((Boolean) json.get("proxy"))) {
                return true;
            }
        } else {
            // If "proxy" is true, vpn or tor will also be true
            if (json.get("vpn") != null && ((Boolean) json.get("vpn"))) {
                return true;
            }
            if (json.get("tor") != null && ((Boolean) json.get("tor"))) {
                return true;
            }
        }
        if (json.get("bot_status") != null && ((Boolean) json.get("bot_status"))) {
            return true;
        }
        if (sourceConfigNode.getNode("recent-abuse").getBoolean()) {
            if (json.get("recent_abuse") != null && ((Boolean) json.get("recent_abuse"))) {
                return true;
            }
        }
        if (json.get("is_crawler") != null && ((Boolean) json.get("is_crawler"))) {
            return true;
        }

        if (json.get("fraud_score") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        double retVal = ((Number) json.get("fraud_score")).doubleValue();
        if (retVal < 0.0d) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        return retVal >= sourceConfigNode.getNode("threshold").getDouble();
    }
}

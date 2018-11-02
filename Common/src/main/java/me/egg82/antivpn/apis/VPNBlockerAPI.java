package me.egg82.antivpn.apis;

import java.io.IOException;
import java.util.Optional;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VPNBlockerAPI implements API {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "vpnblocker"; }

    public Optional<Boolean> getResult(String ip, ConfigurationNode sourceConfigNode) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (sourceConfigNode == null) {
            throw new IllegalArgumentException("sourceConfigNode cannot be null.");
        }

        String key = sourceConfigNode.getNode("key").getString();

        JSONObject json;
        try {
            json = JSONWebUtil.getJsonObject("http" + ((key != null && !key.isEmpty()) ? "s" : "") + "://api.vpnblocker.net/v2/json/" + ip + ((key != null && !key.isEmpty()) ? "/" + key : ""), "egg82/AntiVPN");
        } catch (IOException | ParseException ex) {
            logger.error(ex.getMessage(), ex);
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }

        String status = (String) json.get("status");
        if (!status.equalsIgnoreCase("success")) {
            return Optional.empty();
        }

        return Optional.of((((Boolean) json.get("host-ip"))) ? Boolean.TRUE : Boolean.FALSE);
    }
}

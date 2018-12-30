package me.egg82.antivpn.apis;

import java.io.IOException;
import java.util.Optional;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyCheckAPI implements API {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String getName() { return "proxycheck"; }

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
            json = JSONWebUtil.getJsonObject("https://proxycheck.io/v2/" + ip + "?vpn=1" + ((key != null && !key.isEmpty()) ? "&key=" + key : ""), "egg82/AntiVPN");
        } catch (IOException | ParseException ex) {
            logger.error(ex.getMessage(), ex);
            return Optional.empty();
        }
        if (json == null || json.get("status") == null) {
            return Optional.empty();
        }

        String status = (String) json.get("status");
        if (!status.equalsIgnoreCase("ok")) {
            return Optional.empty();
        }

        JSONObject result = (JSONObject) json.get(ip);
        if (result == null || result.get("proxy") == null) {
            return Optional.empty();
        }
        String proxy = (String) result.get("proxy");

        return Optional.of(proxy.equalsIgnoreCase("yes") ? Boolean.TRUE : Boolean.FALSE);
    }
}

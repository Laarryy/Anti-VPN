package me.egg82.avpn.apis;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.json.simple.JSONObject;

import me.egg82.avpn.Configuration;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.utils.WebUtil;

public class IPDetectorAPI implements IFetchAPI {
    // vars

    // constructor
    public IPDetectorAPI() {

    }

    // public
    public String getName() {
        return "ipdetector";
    }

    public Optional<Boolean> getResult(String ip) {
        String key = ServiceLocator.getService(Configuration.class).getNode("sources", "ipdetector", "key").getString();
        if (key == null || key.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("API-Key", key);

        JSONObject json = null;
        try {
            json = WebUtil.getJsonObject("https://api.ipdetector.info/" + ip, "egg82/AntiVPN", headers);
        } catch (Exception ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }

        int goodIp = ((Number) json.get("goodIp")).intValue();
        return Optional.of((goodIp == 0) ? Boolean.TRUE : Boolean.FALSE);
    }

    // private

}

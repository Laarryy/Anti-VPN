package me.egg82.avpn.apis;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.json.simple.JSONObject;

import me.egg82.avpn.Configuration;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.utils.WebUtil;

public class IPHubAPI implements IFetchAPI {
    // vars

    // constructor
    public IPHubAPI() {

    }

    // public
    public String getName() {
        return "iphub";
    }

    public Optional<Boolean> getResult(String ip) {
        String key = ServiceLocator.getService(Configuration.class).getNode("sources", "iphub", "key").getString();
        if (key == null || key.isEmpty()) {
            return Optional.empty();
        }

        int blockType = ServiceLocator.getService(Configuration.class).getNode("sources", "iphub", "block").getInt(1);

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-Key", key);

        JSONObject json = null;
        try {
            json = WebUtil.getJsonObject("https://v2.api.iphub.info/ip/" + ip, "egg82/AntiVPN", headers);
        } catch (Exception ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }

        int block = ((Number) json.get("block")).intValue();
        return Optional.of((block == blockType) ? Boolean.TRUE : Boolean.FALSE);
    }

    // private

}

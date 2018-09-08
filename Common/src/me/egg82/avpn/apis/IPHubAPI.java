package me.egg82.avpn.apis;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.json.simple.JSONObject;

import me.egg82.avpn.Configuration;
import me.egg82.avpn.utils.WebUtil;
import ninja.egg82.patterns.ServiceLocator;

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

        JSONObject json = WebUtil.getJson("https://v2.api.iphub.info/ip/" + ip, headers);
        if (json == null) {
            return Optional.empty();
        }

        int block = ((Number) json.get("block")).intValue();
        return Optional.of((block == blockType) ? Boolean.TRUE : Boolean.FALSE);
    }

    // private

}

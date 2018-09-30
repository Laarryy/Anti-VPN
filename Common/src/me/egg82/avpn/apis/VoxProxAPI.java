package me.egg82.avpn.apis;

import java.util.Optional;

import org.json.simple.JSONObject;

import me.egg82.avpn.Configuration;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.utils.WebUtil;

public class VoxProxAPI implements IFetchAPI {
    // vars

    // constructor
    public VoxProxAPI() {

    }

    // public
    public String getName() {
        return "voxprox";
    }

    public Optional<Boolean> getResult(String ip) {
        String key = ServiceLocator.getService(Configuration.class).getNode("sources", "voxprox", "key").getString();
        if (key == null || key.isEmpty()) {
            return Optional.empty();
        }

        JSONObject json = null;
        try {
            json = WebUtil.getJsonObject("https://www.voxprox.com/pmx.php?key=" + key + "&ip=" + ip, "egg82/AntiVPN");
        } catch (Exception ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }

        return Optional.of((((String) json.get("Result")).equalsIgnoreCase("true")) ? Boolean.TRUE : Boolean.FALSE);
    }

    // private

}

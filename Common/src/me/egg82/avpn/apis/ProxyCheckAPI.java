package me.egg82.avpn.apis;

import java.util.Optional;

import org.json.simple.JSONObject;

import me.egg82.avpn.Configuration;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.utils.WebUtil;

public class ProxyCheckAPI implements IFetchAPI {
    // vars

    // constructor
    public ProxyCheckAPI() {

    }

    // public
    public String getName() {
        return "proxycheck";
    }

    public Optional<Boolean> getResult(String ip) {
        String key = ServiceLocator.getService(Configuration.class).getNode("sources", "proxycheck", "key").getString();

        JSONObject json = null;
        try {
            json = WebUtil.getJsonObject("https://proxycheck.io/v2/" + ip + "?vpn=1" + ((key != null && !key.isEmpty()) ? "&key=" + key : ""), "egg82/AntiVPN");
        } catch (Exception ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }

        String status = (String) json.get("status");
        if (!status.equalsIgnoreCase("ok")) {
            return Optional.empty();
        }

        JSONObject result = (JSONObject) json.get(ip);
        String proxy = (String) result.get("proxy");

        return Optional.of(proxy.equalsIgnoreCase("yes") ? Boolean.TRUE : Boolean.FALSE);
    }

    // private

}

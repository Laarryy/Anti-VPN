package me.egg82.avpn.apis;

import java.util.Optional;

import org.json.simple.JSONObject;

import me.egg82.avpn.Configuration;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.utils.WebUtil;

public class VPNBlockerAPI implements IFetchAPI {
    // vars

    // constructor
    public VPNBlockerAPI() {

    }

    // public
    public String getName() {
        return "vpnblocker";
    }

    public Optional<Boolean> getResult(String ip) {
        String key = ServiceLocator.getService(Configuration.class).getNode("sources", "vpnblocker", "key").getString();

        JSONObject json = null;
        try {
            json = WebUtil.getJsonObject("http" + ((key != null && !key.isEmpty()) ? "s" : "") + "://api.vpnblocker.net/v2/json/" + ip + ((key != null && !key.isEmpty()) ? "/" + key : ""),
                "egg82/AntiVPN");
        } catch (Exception ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }

        String status = (String) json.get("status");
        if (!status.equalsIgnoreCase("success")) {
            return Optional.empty();
        }

        return Optional.of((((Boolean) json.get("host-ip")).booleanValue()) ? Boolean.TRUE : Boolean.FALSE);
    }

    // private

}

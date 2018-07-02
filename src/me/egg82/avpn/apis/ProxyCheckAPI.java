package me.egg82.avpn.apis;

import org.json.simple.JSONObject;

import me.egg82.avpn.utils.WebUtil;
import ninja.egg82.bukkit.services.ConfigRegistry;
import ninja.egg82.patterns.ServiceLocator;

public class ProxyCheckAPI {
	//vars
	
	//constructor
	public ProxyCheckAPI() {
		
	}
	
	//public
	public static Boolean isVPN(String ip) {
		String key = ServiceLocator.getService(ConfigRegistry.class).getRegister("sources.proxycheck.key", String.class);
		
		JSONObject json = WebUtil.getJson("https://proxycheck.io/v2/" + ip + "?vpn=1" + ((key != null && !key.isEmpty()) ? "&key=" + key : ""));
		if (json == null) {
			return null;
		}
		
		String status = (String) json.get("status");
		if (!status.equalsIgnoreCase("ok")) {
			return null;
		}
		
		JSONObject result = (JSONObject) json.get(ip);
		String proxy = (String) result.get("proxy");
		
		return proxy.equalsIgnoreCase("yes") ? Boolean.TRUE : Boolean.FALSE;
	}
	
	//private
	
}

package me.egg82.avpn.apis;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import me.egg82.avpn.utils.WebUtil;
import ninja.egg82.bukkit.services.ConfigRegistry;
import ninja.egg82.patterns.ServiceLocator;

public class IPDetectorAPI {
	//vars
	
	//constructor
	public IPDetectorAPI() {
		
	}
	
	//public
	public static Boolean isVPN(String ip) {
		String key = ServiceLocator.getService(ConfigRegistry.class).getRegister("sources.ipdetector.key", String.class);
		if (key == null || key.isEmpty()) {
			return null;
		}
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("API-Key", key);
		
		JSONObject json = WebUtil.getJson("https://api.ipdetector.info/" + ip, headers);
		if (json == null) {
			return null;
		}
		
		int goodIp = ((Integer) json.get("goodIp")).intValue();
		return (goodIp == 0) ? Boolean.TRUE : Boolean.FALSE;
	}
	
	//private
	
}

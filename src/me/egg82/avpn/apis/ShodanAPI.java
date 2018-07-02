package me.egg82.avpn.apis;

import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import me.egg82.avpn.utils.WebUtil;
import ninja.egg82.bukkit.services.ConfigRegistry;
import ninja.egg82.patterns.ServiceLocator;

public class ShodanAPI {
	//vars
	
	//constructor
	public ShodanAPI() {
		
	}
	
	//public
	public static Optional<Boolean> isVPN(String ip) {
		String key = ServiceLocator.getService(ConfigRegistry.class).getRegister("sources.shodan.key", String.class);
		if (key == null || key.isEmpty()) {
			return Optional.empty();
		}
		
		JSONObject json = WebUtil.getJson("https://api.shodan.io/shodan/host/" + ip + "?key=" + ip + key);
		if (json == null) {
			return Optional.empty();
		}
		
		JSONArray tags = (JSONArray) json.get("tags");
		if (tags == null) {
			return Optional.empty();
		}
		
		if (tags.size() == 0) {
			return Optional.of(Boolean.FALSE);
		}
		for (Object tag : tags) {
			String t = (String) tag;
			if (t.equalsIgnoreCase("vpn")) {
				return Optional.of(Boolean.TRUE);
			}
		}
		
		return Optional.of(Boolean.FALSE);
	}
	
	//private
	
}

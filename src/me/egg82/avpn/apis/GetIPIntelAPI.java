package me.egg82.avpn.apis;

import org.json.simple.JSONObject;

import me.egg82.avpn.utils.WebUtil;
import ninja.egg82.bukkit.services.ConfigRegistry;
import ninja.egg82.patterns.ServiceLocator;

public class GetIPIntelAPI {
	//vars
	
	//constructor
	public GetIPIntelAPI() {
		
	}
	
	//public
	public static Double getResult(String ip) {
		JSONObject json = WebUtil.getJson("https://check.getipintel.net/check.php?ip=" + ip + "&contact=" + ServiceLocator.getService(ConfigRegistry.class).getRegister("sources.getipintel.contact", String.class) + "&format=json&flags=b");
		if (json == null) {
			return null;
		}
		
		double retVal = Double.parseDouble((String) json.get("result"));
		if (retVal < 0.0d) {
			return null;
		}
		
		return Double.valueOf(retVal);
	}
	
	//private
	
}

package me.egg82.avpn.apis;

import java.util.Optional;

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
	public static Optional<Double> getResult(String ip) {
		JSONObject json = WebUtil.getJson("https://check.getipintel.net/check.php?ip=" + ip + "&contact=" + ServiceLocator.getService(ConfigRegistry.class).getRegister("sources.getipintel.contact", String.class) + "&format=json&flags=b");
		if (json == null) {
			return Optional.empty();
		}
		
		double retVal = Double.parseDouble((String) json.get("result"));
		if (retVal < 0.0d) {
			return Optional.empty();
		}
		
		return Optional.of(Double.valueOf(retVal));
	}
	
	//private
	
}

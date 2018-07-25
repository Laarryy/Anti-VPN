package me.egg82.avpn.apis;

import java.util.Optional;

import org.json.simple.JSONObject;

import me.egg82.avpn.Configuration;
import me.egg82.avpn.utils.WebUtil;
import ninja.egg82.patterns.ServiceLocator;

public class GetIPIntelAPI implements IFetchAPI {
	//vars
	
	//constructor
	public GetIPIntelAPI() {
		
	}
	
	//public
	public String getName() {
		return "getipintel";
	}
	public Optional<Boolean> getResult(String ip) {
		JSONObject json = WebUtil.getJson("https://check.getipintel.net/check.php?ip=" + ip + "&contact=" + ServiceLocator.getService(Configuration.class).getNode("sources", "getipintel", "contact").getString("") + "&format=json&flags=b");
		if (json == null) {
			return Optional.empty();
		}
		
		double retVal = Double.parseDouble((String) json.get("result"));
		if (retVal < 0.0d) {
			return Optional.empty();
		}
		
		return Optional.of((retVal >= ServiceLocator.getService(Configuration.class).getNode("sources", "getipintel", "threshold").getDouble()) ? Boolean.TRUE : Boolean.FALSE);
	}
	
	//private
	
}

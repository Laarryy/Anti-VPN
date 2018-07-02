package me.egg82.avpn.registries;

import java.util.concurrent.TimeUnit;

import ninja.egg82.bukkit.services.ConfigRegistry;
import ninja.egg82.enums.ExpirationPolicy;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.registries.ExpiringRegistry;
import ninja.egg82.utils.TimeUtil;

public class IPRegistry extends ExpiringRegistry<String, Boolean> {
	//vars
	
	//constructor
	public IPRegistry() {
		super(String.class, Boolean.class, TimeUtil.getTime(ServiceLocator.getService(ConfigRegistry.class).getRegister("cacheTime", String.class)), TimeUnit.MILLISECONDS, ExpirationPolicy.ACCESSED);
	}
	
	//public
	
	//private
	
}

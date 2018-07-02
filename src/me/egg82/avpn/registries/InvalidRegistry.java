package me.egg82.avpn.registries;

import java.util.concurrent.TimeUnit;

import ninja.egg82.patterns.registries.ExpiringRegistry;

public class InvalidRegistry extends ExpiringRegistry<String, Boolean> {
	//vars
	
	//constructor
	public InvalidRegistry() {
		super(String.class, Boolean.class, 60L * 1000L, TimeUnit.MILLISECONDS);
	}
	
	//public
	
	//private
	
}

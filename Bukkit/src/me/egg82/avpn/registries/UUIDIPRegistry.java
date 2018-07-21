package me.egg82.avpn.registries;

import java.util.UUID;

import ninja.egg82.patterns.registries.Registry;

public class UUIDIPRegistry extends Registry<UUID, String> {
	//vars
	
	//constructor
	public UUIDIPRegistry() {
		super(UUID.class, String.class);
	}
	
	//public
	
	//private
	
}

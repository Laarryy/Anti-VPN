package me.egg82.avpn;

import com.google.common.collect.ImmutableSet;

public class Config {
	//vars
	public static volatile ImmutableSet<String> sources = null;
	public static volatile long sourceCacheTime = -1L;
	public static volatile boolean debug = false;
	public static volatile String kickMessage = null;
	public static volatile boolean async = true;
	
	//constructor
	public Config() {
		
	}
	
	//public
	
	//private
	
}

package me.egg82.avpn.debug;

import ninja.egg82.bungeecord.BasePlugin;
import ninja.egg82.patterns.ServiceLocator;

public class BungeeDebugPrinter implements IDebugPrinter {
	//vars
	
	//constructor
	public BungeeDebugPrinter() {
		
	}
	
	//public
	public void printInfo(String message) {
		ServiceLocator.getService(BasePlugin.class).printInfo(message);
	}
	public void printWarning(String message) {
		ServiceLocator.getService(BasePlugin.class).printWarning(message);
	}
	public void printError(String message) {
		ServiceLocator.getService(BasePlugin.class).printError(message);
	}
	
	//private
	
}

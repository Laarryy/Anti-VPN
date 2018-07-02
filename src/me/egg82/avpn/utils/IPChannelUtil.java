package me.egg82.avpn.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import ninja.egg82.exceptionHandlers.IExceptionHandler;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.utils.ChannelUtil;

public class IPChannelUtil {
	//vars
	
	//constructor
	public IPChannelUtil() {
		
	}
	
	//public
	public static void broadcastInfo(String ip, boolean value, long created) {
		if (ip == null) {
			throw new IllegalArgumentException("ip cannot be null.");
		}
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		
		try {
			out.writeUTF(ip);
			out.writeBoolean(value);
			out.writeLong(created);
		} catch (Exception ex) {
			ServiceLocator.getService(IExceptionHandler.class).silentException(ex);
			ex.printStackTrace();
			return;
		}
		
		ChannelUtil.broadcastToServers("AntiVPNIPInfo", stream.toByteArray());
	}
	
	//private
	
}

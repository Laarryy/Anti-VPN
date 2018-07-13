package me.egg82.avpn.core;

import ninja.egg82.patterns.events.EventArgs;

public class ResultEventArgs extends EventArgs {
	//vars
	public static ResultEventArgs EMPTY = new ResultEventArgs(null, null, -1L);
	
	private String ip = null;
	private Boolean value = null;
	private long created = -1L;
	
	//constructor
	public ResultEventArgs(String ip, Boolean value, long created) {
		this.ip = ip;
		this.value = value;
		this.created = created;
	}
	
	//public
	public String getIp() {
		return ip;
	}
	public Boolean getValue() {
		return value;
	}
	public long getCreated() {
		return created;
	}
	
	//private
	
}

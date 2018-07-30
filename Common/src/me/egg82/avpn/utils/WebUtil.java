package me.egg82.avpn.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONObject;

import ninja.egg82.plugin.utils.JSONUtil;

public class WebUtil {
	//vars
	
	//constructor
	public WebUtil() {
		
	}
	
	//public
	public static JSONObject getJson(String url) {
		return getJson(url, "GET", null);
	}
	public static JSONObject getJson(String url, String method) {
		return getJson(url, method, null);
	}
	public static JSONObject getJson(String url, Map<String, String> headers) {
		return getJson(url, "GET", headers);
	}
	public static JSONObject getJson(String url, String method, Map<String, String> headers) {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
		} catch (Exception ex) {
			return null;
		}
		
		conn.setDoOutput(false);
		conn.setDoInput(true);
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Connection", "close");
		conn.setRequestProperty("User-Agent", "egg82/AntiVPN");
		if (headers != null) {
			for (Entry<String, String> kvp : headers.entrySet()) {
				conn.setRequestProperty(kvp.getKey(), kvp.getValue());
			}
		}
		
		try {
			conn.setRequestMethod(method);
		} catch (Exception ex) {
			return null;
		}
		
		try {
			int code = conn.getResponseCode();
			
			try (InputStream in = (code == 200) ? conn.getInputStream() : conn.getErrorStream(); InputStreamReader reader = new InputStreamReader(in); BufferedReader buffer = new BufferedReader(reader)) {
				StringBuilder builder = new StringBuilder();
				String line = null;
				while ((line = buffer.readLine()) != null) {
					builder.append(line);
				}
				
				if (code == 200) {
					return JSONUtil.parseObject(builder.toString());
				}
			}
		} catch (Exception ex) {
			return null;
		}
		
		return null;
	}
	
	//private
	
}

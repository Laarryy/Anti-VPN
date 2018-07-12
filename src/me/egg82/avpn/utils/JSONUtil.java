package me.egg82.avpn.utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ninja.egg82.concurrent.DynamicConcurrentDeque;
import ninja.egg82.concurrent.IConcurrentDeque;

public class JSONUtil {
	//vars
	private static IConcurrentDeque<JSONParser> pool = new DynamicConcurrentDeque<JSONParser>(); // JSONParser is not stateless and thus requires a pool in multi-threaded environments
	
	//constructor
	public JSONUtil() {
		pool.add(new JSONParser());
	}
	
	//public
	public static JSONObject parseObject(String input) throws ParseException, ClassCastException {
		JSONParser parser = getParser();
		JSONObject retVal = (JSONObject) parser.parse(input);
		pool.add(parser);
		return retVal;
	}
	public static JSONArray parseArray(String input) throws ParseException, ClassCastException {
		JSONParser parser = getParser();
		JSONArray retVal = (JSONArray) parser.parse(input);
		pool.add(parser);
		return retVal;
	}
	
	//private
	private static JSONParser getParser() {
		JSONParser parser = pool.pollFirst();
		if (parser == null) {
			parser = new JSONParser();
		}
		return parser;
	}
}

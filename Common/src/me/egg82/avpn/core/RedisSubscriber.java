package me.egg82.avpn.core;

import me.egg82.avpn.utils.IPCacheUtil;
import redis.clients.jedis.JedisPubSub;

public class RedisSubscriber extends JedisPubSub {
    // vars

    // constructor
    public RedisSubscriber() {
        super();
    }

    // public
    public void onMessage(String channel, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        String[] parts = message.split(",\\s?");
        if (parts.length != 3) {
            return;
        }

        if (channel.equals("avpn")) {
            String ip = parts[0];
            boolean value = Boolean.parseBoolean(parts[1]);
            long created = Long.parseLong(parts[2]);

            IPCacheUtil.addToCache(ip, value, created, true);
        } else if (channel.equals("avpn-consensus")) {
            String ip = parts[0];
            double value = Double.parseDouble(parts[1]);
            long created = Long.parseLong(parts[2]);

            IPCacheUtil.addToCache(ip, value, created, true);
        }
    }

    // private

}

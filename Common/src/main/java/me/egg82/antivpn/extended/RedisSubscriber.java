package me.egg82.antivpn.extended;

import java.util.UUID;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.services.InternalAPI;
import me.egg82.antivpn.services.Redis;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.json.JSONUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

public class RedisSubscriber {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public RedisSubscriber() {
        try (Jedis redis = RedisUtil.getRedis()) {
            if (redis == null) {
                return;
            }

            redis.subscribe(new Subscriber(), "antivpn-result", "antivpn-consensus", "antivpn-delete");
        } catch (JedisException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    class Subscriber extends JedisPubSub {
        private Subscriber() { super(); }

        public void onMessage(String channel, String message) {
            if (channel.equals("antivpn-result")) {
                try {
                    JSONObject obj = JSONUtil.parseObject(message);
                    String ip = (String) obj.get("ip");
                    boolean value = (Boolean) obj.get("value");
                    long created = ((Number) obj.get("created")).longValue();
                    UUID id = UUID.fromString((String) obj.get("id"));

                    if (!ValidationUtil.isValidIp(message)) {
                        logger.warn("non-valid IP sent through Redis pub/sub cascade");
                        return;
                    }

                    if (id.equals(Redis.getServerID())) {
                        logger.info("ignoring message sent from this server");
                        return;
                    }

                    InternalAPI.set(ip, value, created);
                } catch (APIException | ParseException | ClassCastException | NullPointerException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } else if (channel.equals("antivpn-consensus")) {
                try {
                    JSONObject obj = JSONUtil.parseObject(message);
                    String ip = (String) obj.get("ip");
                    double value = ((Number) obj.get("value")).doubleValue();
                    long created = ((Number) obj.get("created")).longValue();
                    UUID id = UUID.fromString((String) obj.get("id"));

                    if (!ValidationUtil.isValidIp(message)) {
                        logger.warn("non-valid IP sent through Redis pub/sub consensus");
                        return;
                    }

                    if (id.equals(Redis.getServerID())) {
                        logger.info("ignoring message sent from this server");
                        return;
                    }

                    InternalAPI.set(ip, value, created);
                } catch (APIException | ParseException | ClassCastException | NullPointerException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } else if (channel.equals("antivpn-delete")) {
                // In this case, the message is the "IP"
                try {
                    InternalAPI.delete(message);
                } catch (APIException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }
    }
}

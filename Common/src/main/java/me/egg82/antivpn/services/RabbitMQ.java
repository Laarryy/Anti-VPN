package me.egg82.antivpn.services;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import me.egg82.antivpn.core.ConsensusResult;
import me.egg82.antivpn.core.DataResult;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.utils.ConfigUtil;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitMQ {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQ.class);

    private static final UUID serverId = UUID.randomUUID();
    public static UUID getServerID() { return serverId; }

    private static Charset utf8 = Charset.forName("UTF-8");

    private RabbitMQ() {}

    public static void broadcast(DataResult sqlResult) {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            return;
        }

        try (Channel channel = RabbitMQUtil.getChannel(RabbitMQUtil.getConnection())) {
            if (channel == null) {
                return;
            }

            int offset = (int) Math.floorDiv(System.currentTimeMillis() - sqlResult.getCreated(), 1000L);
            int cacheTime = (int) Math.floorDiv(cachedConfig.get().getSourceCacheTime(), 1000L);

            if (offset < cacheTime) {
                JSONObject obj = new JSONObject();
                obj.put("ip", sqlResult.getIp());
                obj.put("value", sqlResult.getValue());
                obj.put("created", sqlResult.getCreated());
                obj.put("id", serverId.toString());

                channel.exchangeDeclare("antivpn-result", "fanout");
                channel.basicPublish("antivpn-result", "", null, obj.toJSONString().getBytes(utf8));
            } else {
                channel.exchangeDeclare("antivpn-delete", "fanout");
                channel.basicPublish("antivpn-delete", "", null, sqlResult.getIp().getBytes(utf8));
            }
        } catch (IOException | TimeoutException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public static void broadcast(ConsensusResult sqlResult) {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            return;
        }

        try (Channel channel = RabbitMQUtil.getChannel(RabbitMQUtil.getConnection())) {
            if (channel == null) {
                return;
            }

            int offset = (int) Math.floorDiv(System.currentTimeMillis() - sqlResult.getCreated(), 1000L);
            int cacheTime = (int) Math.floorDiv(cachedConfig.get().getSourceCacheTime(), 1000L);

            if (offset < cacheTime) {
                JSONObject obj = new JSONObject();
                obj.put("ip", sqlResult.getIp());
                obj.put("value", sqlResult.getValue());
                obj.put("created", sqlResult.getCreated());
                obj.put("id", serverId.toString());

                channel.exchangeDeclare("antivpn-consensus", "fanout");
                channel.basicPublish("antivpn-consensus", "", null, obj.toJSONString().getBytes(utf8));
            } else {
                channel.exchangeDeclare("antivpn-delete", "fanout");
                channel.basicPublish("antivpn-delete", "", null, sqlResult.getIp().getBytes(utf8));
            }
        } catch (IOException | TimeoutException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}

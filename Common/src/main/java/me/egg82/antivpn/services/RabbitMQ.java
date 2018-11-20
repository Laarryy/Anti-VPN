package me.egg82.antivpn.services;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import me.egg82.antivpn.core.ConsensusResult;
import me.egg82.antivpn.core.DataResult;
import me.egg82.antivpn.utils.RabbitMQUtil;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitMQ {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQ.class);

    private static final UUID serverId = UUID.randomUUID();
    public static UUID getServerID() { return serverId; }

    private static Charset utf8 = Charset.forName("UTF-8");

    private RabbitMQ() {}

    public static CompletableFuture<Boolean> broadcast(DataResult sqlResult, long sourceCacheTime, Connection connection) {
        return CompletableFuture.supplyAsync(() -> {
            try (Channel channel = RabbitMQUtil.getChannel(connection)) {
                if (channel == null) {
                    return Boolean.FALSE;
                }

                int offset = (int) Math.floorDiv((sourceCacheTime + sqlResult.getCreated()) - System.currentTimeMillis(), 1000L);

                if (offset > 0) {
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

                return Boolean.TRUE;
            } catch (IOException | TimeoutException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return Boolean.FALSE;
        });
    }

    public static CompletableFuture<Boolean> broadcast(ConsensusResult sqlResult, long sourceCacheTime, Connection connection) {
        return CompletableFuture.supplyAsync(() -> {
            try (Channel channel = RabbitMQUtil.getChannel(connection)) {
                if (channel == null) {
                    return Boolean.FALSE;
                }

                int offset = (int) Math.floorDiv((sourceCacheTime + sqlResult.getCreated()) - System.currentTimeMillis(), 1000L);

                if (offset > 0) {
                    JSONObject obj = new JSONObject();
                    obj.put("ip", sqlResult.getIp());
                    obj.put("value", sqlResult.getValue());
                    obj.put("created", sqlResult.getCreated());
                    obj.put("id", serverId.toString());

                    channel.exchangeDeclare("antivpn-consensus", "fanout");
                    channel.basicPublish("antivpn-result", "", null, obj.toJSONString().getBytes(utf8));
                } else {
                    channel.exchangeDeclare("antivpn-delete", "fanout");
                    channel.basicPublish("antivpn-delete", "", null, sqlResult.getIp().getBytes(utf8));
                }

                return Boolean.TRUE;
            } catch (IOException | TimeoutException ex) {
                logger.error(ex.getMessage(), ex);
            }

            return Boolean.FALSE;
        });
    }
}

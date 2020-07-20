package me.egg82.antivpn.messaging;

import com.rabbitmq.client.*;
import me.egg82.antivpn.services.MessagingHandler;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.analytics.utils.JSONUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class RabbitMQ implements Messaging {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // TODO: connection recovery?
    // https://www.rabbitmq.com/api-guide.html#recovery
    private ConnectionFactory factory;
    // "Connection" acts as our pool
    // https://stackoverflow.com/questions/10407760/is-there-a-performance-difference-between-pooling-connections-or-channels-in-rab
    private RecoverableConnection connection;

    private String serverID;
    private UUID uuidServerID;
    private MessagingHandler handler;

    private RabbitMQ() { }

    private volatile boolean closed = false;

    public void close() {
        closed = true;
        try {
            connection.close(8000);
        } catch (IOException ignored) { }
    }

    public boolean isClosed() { return closed || !connection.isOpen(); }

    public static RabbitMQ.Builder builder(UUID serverID, MessagingHandler handler) { return new RabbitMQ.Builder(serverID, handler); }

    public static class Builder {
        private final RabbitMQ result = new RabbitMQ();
        private final ConnectionFactory config = new ConnectionFactory();

        private Builder(UUID serverID, MessagingHandler handler) {
            if (serverID == null) {
                throw new IllegalArgumentException("serverID cannot be null.");
            }
            if (handler == null) {
                throw new IllegalArgumentException("handler cannot be null.");
            }

            result.uuidServerID = serverID;
            result.serverID = serverID.toString();
            result.handler = handler;
        }

        public RabbitMQ.Builder url(String address, int port, String vHost) {
            config.setHost(address);
            config.setPort(port);
            config.setVirtualHost(vHost);
            return this;
        }

        public RabbitMQ.Builder credentials(String user, String pass) {
            config.setUsername(user);
            config.setPassword(pass);
            return this;
        }

        public RabbitMQ.Builder timeout(int timeout) {
            config.setConnectionTimeout(timeout);
            return this;
        }

        public RabbitMQ build() throws MessagingException {
            result.factory = config;
            try {
                result.connection = result.getConnection();
                // Bind queues
                result.bind();
            } catch (IOException | TimeoutException ex) {
                throw new MessagingException(false, "Could not create RabbitMQ connection.", ex);
            }
            return result;
        }
    }

    private void bind() throws IOException {
        RecoverableChannel ipChannel = getChannel();
        ipChannel.exchangeDeclare("antivpn-ip", ExchangeType.FANOUT.getType(), true);
        String ipQueue = ipChannel.queueDeclare().getQueue();
        ipChannel.queueBind(ipQueue, "antivpn-ip", "");
        Consumer ipConsumer = new DefaultConsumer(ipChannel) {
            public void handleDelivery(String tag, Envelope envelope, AMQP.BasicProperties props, byte[] body) throws IOException {
                try {
                    receiveIP(props, new String(body, props.getContentEncoding()));
                } catch (ParseException | ClassCastException ex) {
                    logger.warn("Could not parse incoming data.", ex);
                }
            }
        };
        ipChannel.basicConsume(ipQueue, true, ipConsumer);

        RecoverableChannel playerChannel = getChannel();
        playerChannel.exchangeDeclare("antivpn-player", ExchangeType.FANOUT.getType(), true);
        String playerQueue = playerChannel.queueDeclare().getQueue();
        playerChannel.queueBind(playerQueue, "antivpn-player", "");
        Consumer playerConsumer = new DefaultConsumer(playerChannel) {
            public void handleDelivery(String tag, Envelope envelope, AMQP.BasicProperties props, byte[] body) throws IOException {
                try {
                    receivePlayer(props, new String(body, props.getContentEncoding()));
                } catch (ParseException | ClassCastException ex) {
                    logger.warn("Could not parse incoming data.", ex);
                }
            }
        };
        playerChannel.basicConsume(playerQueue, true, playerConsumer);

        RecoverableChannel postVPNChannel = getChannel();
        postVPNChannel.exchangeDeclare("antivpn-post-vpn", ExchangeType.FANOUT.getType(), true);
        String postVPNQueue = postVPNChannel.queueDeclare().getQueue();
        postVPNChannel.queueBind(postVPNQueue, "antivpn-post-vpn", "");
        Consumer postVPNConsumer = new DefaultConsumer(postVPNChannel) {
            public void handleDelivery(String tag, Envelope envelope, AMQP.BasicProperties props, byte[] body) throws IOException {
                try {
                    receivePostVPN(props, new String(body, props.getContentEncoding()));
                } catch (ParseException | ClassCastException ex) {
                    logger.warn("Could not parse incoming data.", ex);
                }
            }
        };
        postVPNChannel.basicConsume(postVPNQueue, true, postVPNConsumer);

        RecoverableChannel postMCLeaksChannel = getChannel();
        postMCLeaksChannel.exchangeDeclare("antivpn-post-mcleaks", ExchangeType.FANOUT.getType(), true);
        String postMCLeaksQueue = postMCLeaksChannel.queueDeclare().getQueue();
        postMCLeaksChannel.queueBind(postMCLeaksQueue, "antivpn-post-mcleaks", "");
        Consumer postMCLeaksConsumer = new DefaultConsumer(postMCLeaksChannel) {
            public void handleDelivery(String tag, Envelope envelope, AMQP.BasicProperties props, byte[] body) throws IOException {
                try {
                    receivePostMCLeaks(props, new String(body, props.getContentEncoding()));
                } catch (ParseException | ClassCastException ex) {
                    logger.warn("Could not parse incoming data.", ex);
                }
            }
        };
        postMCLeaksChannel.basicConsume(postMCLeaksQueue, true, postMCLeaksConsumer);
    }

    public void sendIP(UUID messageID, long longIPID, String ip) throws MessagingException {
        if (messageID == null) {
            throw new IllegalArgumentException("messageID cannot be null.");
        }
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        try (RecoverableChannel channel = getChannel()) {
            JSONObject obj = new JSONObject();
            obj.put("longID", longIPID);
            obj.put("ip", ip);
            AMQP.BasicProperties props = getProperties(DeliveryMode.PERSISTENT);
            channel.exchangeDeclare("antivpn-ip", ExchangeType.FANOUT.getType(), true);
            channel.basicPublish("antivpn-ip", "", props, obj.toJSONString().getBytes(props.getContentEncoding()));
        } catch (IOException ex) {
            throw new MessagingException(false, ex);
        } catch (TimeoutException ex) {
            throw new MessagingException(true, ex);
        }
    }

    public void sendPlayer(UUID messageID, long longPlayerID, UUID playerID) throws MessagingException {
        if (messageID == null) {
            throw new IllegalArgumentException("messageID cannot be null.");
        }
        if (playerID == null) {
            throw new IllegalArgumentException("playerID cannot be null.");
        }

        try (RecoverableChannel channel = getChannel()) {
            JSONObject obj = new JSONObject();
            obj.put("longID", longPlayerID);
            obj.put("id", playerID.toString());
            AMQP.BasicProperties props = getProperties(DeliveryMode.PERSISTENT);
            channel.exchangeDeclare("antivpn-player", ExchangeType.FANOUT.getType(), true);
            channel.basicPublish("antivpn-player", "", props, obj.toJSONString().getBytes(props.getContentEncoding()));
        } catch (IOException ex) {
            throw new MessagingException(false, ex);
        } catch (TimeoutException ex) {
            throw new MessagingException(true, ex);
        }
    }

    public void sendPostVPN(UUID messageID, long id, long longIPID, String ip, Optional<Boolean> cascade, Optional<Double> consensus, long created) throws MessagingException {
        if (messageID == null) {
            throw new IllegalArgumentException("messageID cannot be null.");
        }
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }
        if (cascade == null) {
            throw new IllegalArgumentException("cascade cannot be null.");
        }
        if (consensus == null) {
            throw new IllegalArgumentException("consensus cannot be null.");
        }

        try (RecoverableChannel channel = getChannel()) {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("longIPID", longIPID);
            obj.put("ip", ip);
            obj.put("cascade", cascade.orElse(null));
            obj.put("consensus", consensus.orElse(null));
            obj.put("created", created);
            AMQP.BasicProperties props = getProperties(DeliveryMode.PERSISTENT);
            channel.exchangeDeclare("antivpn-post-vpn", ExchangeType.FANOUT.getType(), true);
            channel.basicPublish("antivpn-post-vpn", "", props, obj.toJSONString().getBytes(props.getContentEncoding()));
        } catch (IOException ex) {
            throw new MessagingException(false, ex);
        } catch (TimeoutException ex) {
            throw new MessagingException(true, ex);
        }
    }

    public void sendPostMCLeaks(UUID messageID, long id, long longPlayerID, UUID playerID, boolean value, long created) throws MessagingException {
        if (messageID == null) {
            throw new IllegalArgumentException("messageID cannot be null.");
        }
        if (playerID == null) {
            throw new IllegalArgumentException("playerID cannot be null.");
        }

        try (RecoverableChannel channel = getChannel()) {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("longPlayerID", longPlayerID);
            obj.put("playerID", playerID.toString());
            obj.put("value", value);
            obj.put("created", created);
            AMQP.BasicProperties props = getProperties(DeliveryMode.PERSISTENT);
            channel.exchangeDeclare("antivpn-post-mcleaks", ExchangeType.FANOUT.getType(), true);
            channel.basicPublish("antivpn-post-mcleaks", "", props, obj.toJSONString().getBytes(props.getContentEncoding()));
        } catch (IOException ex) {
            throw new MessagingException(false, ex);
        } catch (TimeoutException ex) {
            throw new MessagingException(true, ex);
        }
    }

    private AMQP.BasicProperties getProperties(DeliveryMode deliveryMode) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("sender", serverID);

        AMQP.BasicProperties.Builder retVal = new AMQP.BasicProperties.Builder();
        retVal.contentType("application/json");
        retVal.contentEncoding(StandardCharsets.UTF_8.name());
        retVal.messageId(UUID.randomUUID().toString());
        retVal.deliveryMode(deliveryMode.getMode());
        retVal.headers(headers);
        return retVal.build();
    }

    private void receiveIP(AMQP.BasicProperties props, String json) throws UnsupportedEncodingException, ParseException, ClassCastException {
        if (props.getHeaders() == null || props.getHeaders().isEmpty()) {
            logger.warn("Properties for received IP was null or empty.");
            return;
        }
        String sender = new String(((LongString) props.getHeaders().get("sender")).getBytes(), props.getContentEncoding());
        if (!ValidationUtil.isValidUuid(sender)) {
            logger.warn("Non-valid sender received in IP: \"" + sender + "\".");
            return;
        }
        if (serverID.equals(sender)) {
            return;
        }

        if (!ValidationUtil.isValidUuid(props.getMessageId())) {
            logger.warn("Non-valid message ID received in IP: \"" + props.getMessageId() + "\".");
            return;
        }

        JSONObject obj = JSONUtil.parseObject(json);
        String ip = (String) obj.get("ip");
        if (!ValidationUtil.isValidIp(ip)) {
            logger.warn("Non-valid IP received in IP: \"" + ip + "\".");
            return;
        }

        handler.ipCallback(
                UUID.fromString(props.getMessageId()),
                ip,
                ((Number) obj.get("longID")).longValue(),
                this
        );
    }

    private void receivePlayer(AMQP.BasicProperties props, String json) throws UnsupportedEncodingException, ParseException, ClassCastException {
        if (props.getHeaders() == null || props.getHeaders().isEmpty()) {
            logger.warn("Properties for received player was null or empty.");
            return;
        }
        String sender = new String(((LongString) props.getHeaders().get("sender")).getBytes(), props.getContentEncoding());
        if (!ValidationUtil.isValidUuid(sender)) {
            logger.warn("Non-valid sender received in player: \"" + sender + "\".");
            return;
        }
        if (serverID.equals(sender)) {
            return;
        }

        if (!ValidationUtil.isValidUuid(props.getMessageId())) {
            logger.warn("Non-valid message ID received in player: \"" + props.getMessageId() + "\".");
            return;
        }

        JSONObject obj = JSONUtil.parseObject(json);
        String id = (String) obj.get("id");
        if (!ValidationUtil.isValidUuid(id)) {
            logger.warn("Non-valid UUID received in player: \"" + id + "\".");
            return;
        }

        handler.playerCallback(
                UUID.fromString(props.getMessageId()),
                UUID.fromString(id),
                ((Number) obj.get("longID")).longValue(),
                this
        );
    }

    private void receivePostVPN(AMQP.BasicProperties props, String json) throws UnsupportedEncodingException, ParseException, ClassCastException {
        if (props.getHeaders() == null || props.getHeaders().isEmpty()) {
            logger.warn("Properties for received post VPN was null or empty.");
            return;
        }
        String sender = new String(((LongString) props.getHeaders().get("sender")).getBytes(), props.getContentEncoding());
        if (!ValidationUtil.isValidUuid(sender)) {
            logger.warn("Non-valid sender received in post VPN: \"" + sender + "\".");
            return;
        }
        if (serverID.equals(sender)) {
            return;
        }

        if (!ValidationUtil.isValidUuid(props.getMessageId())) {
            logger.warn("Non-valid message ID received in post VPN: \"" + props.getMessageId() + "\".");
            return;
        }

        JSONObject obj = JSONUtil.parseObject(json);
        String ip = (String) obj.get("ip");
        if (!ValidationUtil.isValidIp(ip)) {
            logger.warn("Non-valid IP received in post VPN: \"" + ip + "\".");
            return;
        }

        handler.postVPNCallback(
                UUID.fromString(props.getMessageId()),
                ((Number) obj.get("id")).longValue(),
                ((Number) obj.get("longIPID")).longValue(),
                ip,
                obj.get("cascade") == null ? Optional.empty() : Optional.of((Boolean) obj.get("cascade")),
                obj.get("consensus") == null ? Optional.empty() : Optional.of(((Number) obj.get("consensus")).doubleValue()),
                ((Number) obj.get("created")).longValue(),
                this
        );
    }

    private void receivePostMCLeaks(AMQP.BasicProperties props, String json) throws UnsupportedEncodingException, ParseException, ClassCastException {
        if (props.getHeaders() == null || props.getHeaders().isEmpty()) {
            logger.warn("Properties for received post MCLeaks was null or empty.");
            return;
        }
        String sender = new String(((LongString) props.getHeaders().get("sender")).getBytes(), props.getContentEncoding());
        if (!ValidationUtil.isValidUuid(sender)) {
            logger.warn("Non-valid sender received in post MCLeaks: \"" + sender + "\".");
            return;
        }
        if (serverID.equals(sender)) {
            return;
        }

        if (!ValidationUtil.isValidUuid(props.getMessageId())) {
            logger.warn("Non-valid message ID received in post MCLeaks: \"" + props.getMessageId() + "\".");
            return;
        }

        JSONObject obj = JSONUtil.parseObject(json);
        String playerID = (String) obj.get("playerID");
        if (!ValidationUtil.isValidUuid(playerID)) {
            logger.warn("Non-valid UUID received in post MCLeaks: \"" + playerID + "\".");
            return;
        }

        handler.postMCLeaksCallback(
                UUID.fromString(props.getMessageId()),
                ((Number) obj.get("id")).longValue(),
                ((Number) obj.get("longPlayerID")).longValue(),
                UUID.fromString(playerID),
                (Boolean) obj.get("value"),
                ((Number) obj.get("created")).longValue(),
                this
        );
    }

    private RecoverableConnection getConnection() throws IOException, TimeoutException { return (RecoverableConnection) factory.newConnection(); }

    private RecoverableChannel getChannel() throws IOException { return (RecoverableChannel) connection.createChannel(); }

    private enum DeliveryMode {
        /**
         * Not logged to disk
         */
        TRANSIENT(1),
        /**
         * When in a durable exchange, logged to disk
         */
        PERSISTENT(2);

        private final int mode;
        DeliveryMode(int mode) { this.mode = mode; }
        public int getMode() { return mode; }
    }

    private enum ExchangeType {
        DIRECT("direct"),
        FANOUT("fanout"),
        TOPIC("topic"),
        HEADERS("match"); // AMQP compatibility

        private final String type;
        ExchangeType(String type) { this.type = type; }
        public String getType() { return type; }
    }
}

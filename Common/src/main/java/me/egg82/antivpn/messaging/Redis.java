package me.egg82.antivpn.messaging;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.egg82.antivpn.services.MessagingHandler;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.analytics.utils.JSONUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Redis extends JedisPubSub implements Messaging {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ExecutorService workPool = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("AntiVPN-Redis-%d").build());

    private JedisPool pool;

    private String serverID;
    private UUID uuidServerID;
    private MessagingHandler handler;

    private Redis() { }

    private volatile boolean closed = false;

    public void close() {
        closed = true;
        workPool.shutdown();
        try {
            if (!workPool.awaitTermination(4L, TimeUnit.SECONDS)) {
                workPool.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        pool.close();
    }

    public boolean isClosed() { return closed || pool.isClosed(); }

    public static Redis.Builder builder(UUID serverID, MessagingHandler handler) { return new Redis.Builder(serverID, handler); }

    public static class Builder {
        private final Logger logger = LoggerFactory.getLogger(getClass());

        private final Redis result = new Redis();
        private final JedisPoolConfig config = new JedisPoolConfig();

        private String address = "127.0.0.1";
        private int port = 6379;
        private int timeout = 5000;
        private String pass = "";

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

        public Redis.Builder url(String address, int port) {
            this.address = address;
            this.port = port;
            return this;
        }

        public Redis.Builder credentials(String pass) {
            this.pass = pass;
            return this;
        }

        public Redis.Builder poolSize(int min, int max) {
            config.setMinIdle(min);
            config.setMaxTotal(max);
            return this;
        }

        public Redis.Builder life(long lifetime, int timeout) {
            config.setMinEvictableIdleTimeMillis(lifetime);
            config.setMaxWaitMillis(timeout);
            this.timeout = timeout;
            return this;
        }

        public Redis build() throws MessagingException {
            result.pool = new JedisPool(config, address, port, timeout, pass == null || pass.isEmpty() ? null : pass);
            // Warm up pool
            // https://partners-intl.aliyun.com/help/doc-detail/98726.htm
            warmup(result.pool);
            // Indefinite subscription
            subscribe();
            return result;
        }

        private void subscribe() {
            result.workPool.execute(() -> {
                while (!result.isClosed()) {
                    try (Jedis redis = result.pool.getResource()) {
                        redis.subscribe(result,
                                "antivpn-ip",
                                "antivpn-player",
                                "antivpn-post-vpn",
                                "antivpn-post-mcleaks"
                        );
                    } catch (JedisException ex) {
                        if (!result.isClosed()) {
                            logger.warn("Redis pub/sub disconnected. Reconnecting..");
                        }
                    }
                }
            });
        }

        private void warmup(JedisPool pool) throws MessagingException {
            Jedis[] warmpupArr = new Jedis[config.getMinIdle()];

            for (int i = 0; i < config.getMinIdle(); i++) {
                Jedis jedis;
                try {
                    jedis = pool.getResource();
                    warmpupArr[i] = jedis;
                    jedis.ping();
                } catch (JedisException ex) {
                    throw new MessagingException(false, "Could not warm up Redis connection.", ex);
                }
            }
            // Two loops because we need to ensure we don't pull a freshly-created resource from the pool
            for (int i = 0; i < config.getMinIdle(); i++) {
                Jedis jedis;
                try {
                    jedis = warmpupArr[i];
                    jedis.close();
                } catch (JedisException ex) {
                    throw new MessagingException(false, "Could not close warmed Redis connection.", ex);
                }
            }
        }
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

        try (Jedis redis = pool.getResource()) {
            JSONObject obj = createJSON(messageID);
            obj.put("longID", longIPID);
            obj.put("ip", ip);
            redis.publish("antivpn-ip", obj.toJSONString());
        } catch (JedisException ex) {
            throw new MessagingException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public void sendPlayer(UUID messageID, long longPlayerID, UUID playerID) throws MessagingException {
        if (messageID == null) {
            throw new IllegalArgumentException("messageID cannot be null.");
        }
        if (playerID == null) {
            throw new IllegalArgumentException("playerID cannot be null.");
        }

        try (Jedis redis = pool.getResource()) {
            JSONObject obj = createJSON(messageID);
            obj.put("longID", longPlayerID);
            obj.put("id", playerID.toString());
            redis.publish("antivpn-player", obj.toJSONString());
        } catch (JedisException ex) {
            throw new MessagingException(isAutomaticallyRecoverable(ex), ex);
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

        try (Jedis redis = pool.getResource()) {
            JSONObject obj = createJSON(messageID);
            obj.put("id", id);
            obj.put("longIPID", longIPID);
            obj.put("ip", ip);
            obj.put("cascade", cascade.orElse(null));
            obj.put("consensus", consensus.orElse(null));
            obj.put("created", created);
            redis.publish("antivpn-post-vpn", obj.toJSONString());
        } catch (JedisException ex) {
            throw new MessagingException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public void sendPostMCLeaks(UUID messageID, long id, long longPlayerID, UUID playerID, boolean value, long created) throws MessagingException {
        if (messageID == null) {
            throw new IllegalArgumentException("messageID cannot be null.");
        }
        if (playerID == null) {
            throw new IllegalArgumentException("playerID cannot be null.");
        }

        try (Jedis redis = pool.getResource()) {
            JSONObject obj = createJSON(messageID);
            obj.put("id", id);
            obj.put("longPlayerID", longPlayerID);
            obj.put("playerID", playerID.toString());
            obj.put("value", value);
            obj.put("created", created);
            redis.publish("antivpn-post-vpn", obj.toJSONString());
        } catch (JedisException ex) {
            throw new MessagingException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    private JSONObject createJSON(UUID messageID) {
        JSONObject retVal = new JSONObject();
        retVal.put("sender", serverID);
        retVal.put("messageID", messageID.toString());
        return retVal;
    }

    private boolean isAutomaticallyRecoverable(JedisException ex) {
        if (
                ex.getMessage().startsWith("Failed connecting")
                || ex.getMessage().contains("broken connection")
        ) {
            return true;
        }
        return false;
    }

    public void onMessage(String channel, String message) {
        try {
            switch (channel) {
                case "antivpn-ip":
                    receiveIP(message);
                    break;
                case "antivpn-player":
                    receivePlayer(message);
                    break;
                case "antivpn-post-vpn":
                    receivePostVPN(message);
                    break;
                case "antivpn-post-mcleaks":
                    receivePostMCLeaks(message);
                    break;
                default:
                    logger.warn("Got data from channel that should not exist.");
                    break;
            }
        } catch (ParseException | ClassCastException ex) {
            logger.warn("Could not parse incoming data.", ex);
        }
    }

    private void receiveIP(String json) throws ParseException, ClassCastException {
        JSONObject obj = JSONUtil.parseObject(json);
        String sender = (String) obj.get("sender");
        if (!ValidationUtil.isValidUuid(sender)) {
            logger.warn("Non-valid sender received in IP: \"" + sender + "\".");
            return;
        }
        if (serverID.equals(sender)) {
            return;
        }

        String messageID = (String) obj.get("messageID");
        if (!ValidationUtil.isValidUuid(messageID)) {
            logger.warn("Non-valid message ID received in IP: \"" + messageID + "\".");
            return;
        }

        String ip = (String) obj.get("ip");
        if (!ValidationUtil.isValidIp(ip)) {
            logger.warn("Non-valid IP received in IP: \"" + ip + "\".");
            return;
        }

        handler.ipCallback(
                UUID.fromString(messageID),
                ip,
                ((Number) obj.get("longID")).longValue(),
                this
        );
    }

    private void receivePlayer(String json) throws ParseException, ClassCastException {
        JSONObject obj = JSONUtil.parseObject(json);
        String sender = (String) obj.get("sender");
        if (!ValidationUtil.isValidUuid(sender)) {
            logger.warn("Non-valid sender received in player: \"" + sender + "\".");
            return;
        }
        if (serverID.equals(sender)) {
            return;
        }

        String messageID = (String) obj.get("messageID");
        if (!ValidationUtil.isValidUuid(messageID)) {
            logger.warn("Non-valid message ID received in player: \"" + messageID + "\".");
            return;
        }

        String id = (String) obj.get("id");
        if (!ValidationUtil.isValidUuid(id)) {
            logger.warn("Non-valid UUID received in player: \"" + id + "\".");
            return;
        }

        handler.playerCallback(
                UUID.fromString(messageID),
                UUID.fromString(id),
                ((Number) obj.get("longID")).longValue(),
                this
        );
    }

    private void receivePostVPN(String json) throws ParseException, ClassCastException {
        JSONObject obj = JSONUtil.parseObject(json);
        String sender = (String) obj.get("sender");
        if (!ValidationUtil.isValidUuid(sender)) {
            logger.warn("Non-valid sender received in post VPN: \"" + sender + "\".");
            return;
        }
        if (serverID.equals(sender)) {
            return;
        }

        String messageID = (String) obj.get("messageID");
        if (!ValidationUtil.isValidUuid(messageID)) {
            logger.warn("Non-valid message ID received in post VPN: \"" + messageID + "\".");
            return;
        }

        String ip = (String) obj.get("ip");
        if (!ValidationUtil.isValidIp(ip)) {
            logger.warn("Non-valid IP received in post VPN: \"" + ip + "\".");
            return;
        }

        handler.postVPNCallback(
                UUID.fromString(messageID),
                ((Number) obj.get("id")).longValue(),
                ((Number) obj.get("longIPID")).longValue(),
                ip,
                obj.get("cascade") == null ? Optional.empty() : Optional.of((Boolean) obj.get("cascade")),
                obj.get("consensus") == null ? Optional.empty() : Optional.of(((Number) obj.get("consensus")).doubleValue()),
                ((Number) obj.get("created")).longValue(),
                this
        );
    }

    private void receivePostMCLeaks(String json) throws ParseException, ClassCastException {
        JSONObject obj = JSONUtil.parseObject(json);
        String sender = (String) obj.get("sender");
        if (!ValidationUtil.isValidUuid(sender)) {
            logger.warn("Non-valid sender received in post MCLeaks: \"" + sender + "\".");
            return;
        }
        if (serverID.equals(sender)) {
            return;
        }

        String messageID = (String) obj.get("messageID");
        if (!ValidationUtil.isValidUuid(messageID)) {
            logger.warn("Non-valid message ID received in post MCLeaks: \"" + messageID + "\".");
            return;
        }

        String playerID = (String) obj.get("playerID");
        if (!ValidationUtil.isValidUuid(playerID)) {
            logger.warn("Non-valid UUID received in post MCLeaks: \"" + playerID + "\".");
            return;
        }

        handler.postMCLeaksCallback(
                UUID.fromString(messageID),
                ((Number) obj.get("id")).longValue(),
                ((Number) obj.get("longPlayerID")).longValue(),
                UUID.fromString(playerID),
                (Boolean) obj.get("value"),
                ((Number) obj.get("created")).longValue(),
                this
        );
    }
}

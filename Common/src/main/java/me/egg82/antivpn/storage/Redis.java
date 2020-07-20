package me.egg82.antivpn.storage;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import me.egg82.antivpn.core.*;
import me.egg82.antivpn.services.StorageHandler;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.analytics.utils.JSONUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Redis implements Storage {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LoadingCache<String, Long> longIPIDCache = Caffeine.newBuilder().build(this::getLongIPIDExpensive);
    private final LoadingCache<UUID, Long> longPlayerIDCache = Caffeine.newBuilder().build(this::getLongPlayerIDExpensive);

    private JedisPool pool;

    private AtomicLong lastVPNID;
    private AtomicLong lastMCLeaksID;
    private StorageHandler handler;
    protected String prefix = "";

    private Redis() { }

    private volatile boolean closed = false;

    public void close() {
        closed = true;
        pool.close();
    }

    public boolean isClosed() { return closed || pool.isClosed(); }

    public static Redis.Builder builder(StorageHandler handler) { return new Redis.Builder(handler); }

    public static class Builder {
        private final Logger logger = LoggerFactory.getLogger(getClass());

        private final Redis result = new Redis();
        private final JedisPoolConfig config = new JedisPoolConfig();

        private String address = "127.0.0.1";
        private int port = 6379;
        private int timeout = 5000;
        private String pass = "";

        private Builder(StorageHandler handler) {
            if (handler == null) {
                throw new IllegalArgumentException("handler cannot be null.");
            }

            result.handler = handler;
        }

        public Redis.Builder url(String address, int port, String prefix) {
            this.address = address;
            this.port = port;
            result.prefix = prefix;
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

        public Redis build() throws StorageException {
            result.pool = new JedisPool(config, address, port, timeout, pass == null || pass.isEmpty() ? null : pass);
            // Warm up pool
            // https://partners-intl.aliyun.com/help/doc-detail/98726.htm
            warmup(result.pool);
            setDefaults();
            result.lastVPNID = new AtomicLong(getLastVPNID());
            result.lastMCLeaksID = new AtomicLong(getLastMCLeaksID());
            return result;
        }

        private void setDefaults() {
            try (Jedis redis = result.pool.getResource()) {
                redis.setnx(result.prefix + "ips:idx", "0");
                redis.setnx(result.prefix + "players:idx", "0");
                redis.setnx(result.prefix + "vpn_values:idx", "0");
                redis.setnx(result.prefix + "mcleaks_values:idx", "0");
            }
        }

        private void warmup(JedisPool pool) throws StorageException {
            Jedis[] warmpupArr = new Jedis[config.getMinIdle()];

            for (int i = 0; i < config.getMinIdle(); i++) {
                Jedis jedis;
                try {
                    jedis = pool.getResource();
                    warmpupArr[i] = jedis;
                    jedis.ping();
                } catch (JedisException ex) {
                    throw new StorageException(false, "Could not warm up Redis connection.", ex);
                }
            }
            // Two loops because we need to ensure we don't pull a freshly-created resource from the pool
            for (int i = 0; i < config.getMinIdle(); i++) {
                Jedis jedis;
                try {
                    jedis = warmpupArr[i];
                    jedis.close();
                } catch (JedisException ex) {
                    throw new StorageException(false, "Could not close warmed Redis connection.", ex);
                }
            }
        }

        private long getLastVPNID() throws StorageException {
            try (Jedis redis = result.pool.getResource()) {
                long id = Long.parseLong(redis.get(result.prefix + "vpn_values:idx"));
                while (redis.exists(result.prefix + "vpn_values:" + (id + 1))) {
                    id = redis.incr(result.prefix + "vpn_values:idx");
                }
                return id;
            } catch (JedisException ex) {
                throw new StorageException(false, "Could not get last VPN ID.");
            }
        }

        private long getLastMCLeaksID() throws StorageException {
            try (Jedis redis = result.pool.getResource()) {
                long id = Long.parseLong(redis.get(result.prefix + "mcleaks_values:idx"));
                while (redis.exists(result.prefix + "mcleaks_values:" + (id + 1))) {
                    id = redis.incr(result.prefix + "mcleaks_values:idx");
                }
                return id;
            } catch (JedisException ex) {
                throw new StorageException(false, "Could not get last MCLeaks ID.");
            }
        }
    }

    public Set<VPNResult> getVPNQueue() throws StorageException {
        Set<VPNResult> retVal = new LinkedHashSet<>();

        try (Jedis redis = pool.getResource()) {
            long max = Long.parseLong(redis.get(prefix + "vpn_values:idx"));
            while (redis.exists(prefix + "vpn_values:" + (max + 1))) {
                max = redis.incr(prefix + "vpn_values:idx");
            }

            if (lastVPNID.get() >= max) {
                lastVPNID.set(max);
                return retVal;
            }

            long next;
            while ((next = lastVPNID.getAndIncrement()) < max) {
                VPNResult r = null;
                try {
                    r = getVPNResult(next, redis.get(prefix + "vpn_values:" + next), redis);
                } catch (StorageException | JedisException | ParseException | ClassCastException ex) {
                    logger.warn("Could not get VPN data for ID " + next + ".", ex);
                }
                if (r != null) {
                    retVal.add(r);
                }
            }

            return retVal;
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public Set<MCLeaksResult> getMCLeaksQueue() throws StorageException {
        Set<MCLeaksResult> retVal = new LinkedHashSet<>();

        try (Jedis redis = pool.getResource()) {
            long max = Long.parseLong(redis.get(prefix + "mcleaks_values:idx"));
            while (redis.exists(prefix + "mcleaks_values:" + (max + 1))) {
                max = redis.incr(prefix + "mcleaks_values:idx");
            }

            if (lastMCLeaksID.get() >= max) {
                lastMCLeaksID.set(max);
                return retVal;
            }

            long next;
            while ((next = lastMCLeaksID.getAndIncrement()) < max) {
                MCLeaksResult r = null;
                try {
                    r = getMCLeaksResult(next, redis.get(prefix + "mcleaks_values:" + next), redis);
                } catch (StorageException | JedisException | ParseException | ClassCastException ex) {
                    logger.warn("Could not get MCLeaks data for ID " + next + ".", ex);
                }
                if (r != null) {
                    retVal.add(r);
                }
            }

            return retVal;
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public VPNResult getVPNByIP(String ip, long cacheTimeMillis) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            long longIPID = longIPIDCache.get(ip);
            try {
                return getVPNResultIP(longIPID, redis.get(prefix + "vpn_values:ip:" + longIPID), redis, cacheTimeMillis);
            } catch (StorageException | JedisException | ParseException | ClassCastException ex) {
                logger.warn("Could not get VPN data for IP " + longIPID + ".", ex);
            }

            return null;
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public MCLeaksResult getMCLeaksByPlayer(UUID playerID, long cacheTimeMillis) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            long longPlayerID = longPlayerIDCache.get(playerID);
            try {
                return getMCLeaksResultPlayer(longPlayerID, redis.get(prefix + "mcleaks_values:player:" + longPlayerID), redis, cacheTimeMillis);
            } catch (StorageException | JedisException | ParseException | ClassCastException ex) {
                logger.warn("Could not get MCLeaks data for player " + longPlayerID + ".", ex);
            }

            return null;
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public PostVPNResult postVPN(String ip, Optional<Boolean> cascade, Optional<Double> consensus) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            long longIPID = longIPIDCache.get(ip);

            JSONObject obj = new JSONObject();
            obj.put("ipID", longIPID);
            obj.put("cascade", cascade.orElse(null));
            obj.put("consensus", consensus.orElse(null));

            long id;
            long created;
            do {
                do {
                    id = redis.incr(prefix + "vpn_values:idx");
                } while (redis.exists(prefix + "vpn_values:" + id));
                created = getTime(redis.time());
                obj.put("created", created);
            } while (redis.setnx(prefix + "vpn_values:" + id, obj.toJSONString()) == 0L);

            obj.remove("ipID");
            obj.put("id", id);
            redis.rpush(prefix + "vpn:ip:" + longIPID, obj.toJSONString());

            return new PostVPNResult(
                    id,
                    longIPID,
                    ip,
                    cascade,
                    consensus,
                    created
            );
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public PostMCLeaksResult postMCLeaks(UUID playerID, boolean value) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            long longPlayerID = longPlayerIDCache.get(playerID);

            JSONObject obj = new JSONObject();
            obj.put("playerID", longPlayerID);
            obj.put("result", value);

            long id;
            long created;
            do {
                do {
                    id = redis.incr(prefix + "mcleaks_values:idx");
                } while (redis.exists(prefix + "mcleaks_values:" + id));
                created = getTime(redis.time());
                obj.put("created", created);
            } while (redis.setnx(prefix + "mcleaks_values:" + id, obj.toJSONString()) == 0L);

            obj.remove("playerID");
            obj.put("id", id);
            redis.rpush(prefix + "mcleaks_values:player:" + longPlayerID, obj.toJSONString());

            return new PostMCLeaksResult(
                    id,
                    longPlayerID,
                    playerID,
                    value,
                    created
            );
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public void setIPRaw(long longIPID, String ip) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            JSONObject obj = new JSONObject();
            obj.put("ip", ip);

            JSONObject obj2 = new JSONObject();
            obj2.put("longID", longIPID);

            redis.mset(
                    prefix + "ips:" + longIPID, obj.toJSONString(),
                    prefix + "ip:" + ip, obj2.toJSONString()
            );
            longIPIDCache.put(ip, longIPID);
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public void setPlayerRaw(long longPlayerID, UUID playerID) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            JSONObject obj = new JSONObject();
            obj.put("id", playerID.toString());

            JSONObject obj2 = new JSONObject();
            obj2.put("longID", longPlayerID);

            redis.mset(
                    prefix + "players:" + longPlayerID, obj.toJSONString(),
                    prefix + "players:" + playerID.toString(), obj2.toJSONString()
            );
            longPlayerIDCache.put(playerID, longPlayerID);
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public void postVPNRaw(long id, long longIPID, Optional<Boolean> cascade, Optional<Double> consensus, long created) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            JSONObject obj = new JSONObject();
            obj.put("ipID", longIPID);
            obj.put("cascade", cascade.orElse(null));
            obj.put("consensus", consensus.orElse(null));
            obj.put("created", created);

            redis.set(prefix + "vpn_values:" + id, obj.toJSONString());

            obj.remove("ipID");
            obj.put("id", id);
            redis.rpush(prefix + "vpn_values:ip:" + longIPID, obj.toJSONString());
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public void postMCLeaksRaw(long id, long longPlayerID, boolean value, long created) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            JSONObject obj = new JSONObject();
            obj.put("playerID", longPlayerID);
            obj.put("result", value);
            obj.put("created", created);

            redis.set(prefix + "mcleaks_values:" + id, obj.toJSONString());

            obj.remove("playerID");
            obj.put("id", id);
            redis.rpush(prefix + "mcleaks_values:player:" + longPlayerID, obj.toJSONString());
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public long getLongIPID(String ip) { return longIPIDCache.get(ip); }

    public long getLongPlayerID(UUID playerID) { return longPlayerIDCache.get(playerID); }

    public Set<IPResult> dumpIPs(long begin, int size) throws StorageException {
        Set<IPResult> retVal = new LinkedHashSet<>();

        try (Jedis redis = pool.getResource()) {
            for (long i = begin; i < begin + size; i++) {
                IPResult r = null;
                try {
                    String json = redis.get(prefix + "ips:" + i);
                    if (json == null) {
                        continue;
                    }
                    JSONObject obj = JSONUtil.parseObject(json);
                    String ip = (String) obj.get("ip");
                    if (!ValidationUtil.isValidIp(ip)) {
                        logger.warn("Player ID " + i + " has an invalid IP \"" + ip + "\".");
                        continue;
                    }

                    r = new IPResult(
                            i,
                            ip
                    );
                } catch (ParseException | ClassCastException ex) {
                    logger.warn("Could not get IP data for ID " + i + ".", ex);
                }
                if (r != null) {
                    retVal.add(r);
                }
            }

            return retVal;
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public void loadIPs(Set<IPResult> ips, boolean truncate) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            if (truncate) {
                deleteNamespace(redis, prefix + "ips:");
                longIPIDCache.invalidateAll();
            }
            long max = 0;
            for (IPResult ip : ips) {
                max = Math.max(max, ip.getLongIPID());

                JSONObject obj = new JSONObject();
                obj.put("ip", ip.getIP());

                JSONObject obj2 = new JSONObject();
                obj2.put("longID", ip.getLongIPID());

                redis.mset(
                        prefix + "ips:" + ip.getLongIPID(), obj.toJSONString(),
                        prefix + "ips:" + ip.getIP(), obj2.toJSONString()
                );
                longIPIDCache.put(ip.getIP(), ip.getLongIPID());
            }
            redis.set(prefix + "ips:idx", String.valueOf(max));
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public Set<PlayerResult> dumpPlayers(long begin, int size) throws StorageException {
        Set<PlayerResult> retVal = new LinkedHashSet<>();

        try (Jedis redis = pool.getResource()) {
            for (long i = begin; i < begin + size; i++) {
                PlayerResult r = null;
                try {
                    String json = redis.get(prefix + "players:" + i);
                    if (json == null) {
                        continue;
                    }
                    JSONObject obj = JSONUtil.parseObject(json);
                    String pid = (String) obj.get("id");
                    if (!ValidationUtil.isValidUuid(pid)) {
                        logger.warn("Player ID " + i + " has an invalid UUID \"" + pid + "\".");
                        continue;
                    }

                    r = new PlayerResult(
                            i,
                            UUID.fromString(pid)
                    );
                } catch (ParseException | ClassCastException ex) {
                    logger.warn("Could not get player data for ID " + i + ".", ex);
                }
                if (r != null) {
                    retVal.add(r);
                }
            }

            return retVal;
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public void loadPlayers(Set<PlayerResult> players, boolean truncate) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            if (truncate) {
                deleteNamespace(redis, prefix + "players:");
                longPlayerIDCache.invalidateAll();
            }
            long max = 0;
            for (PlayerResult player : players) {
                max = Math.max(max, player.getLongPlayerID());

                JSONObject obj = new JSONObject();
                obj.put("id", player.getPlayerID().toString());

                JSONObject obj2 = new JSONObject();
                obj2.put("longID", player.getLongPlayerID());

                redis.mset(
                        prefix + "players:" + player.getLongPlayerID(), obj.toJSONString(),
                        prefix + "players:" + player.getPlayerID().toString(), obj2.toJSONString()
                );
                longPlayerIDCache.put(player.getPlayerID(), player.getLongPlayerID());
            }
            redis.set(prefix + "players:idx", String.valueOf(max));
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public Set<RawVPNResult> dumpVPNValues(long begin, int size) throws StorageException {
        Set<RawVPNResult> retVal = new LinkedHashSet<>();

        try (Jedis redis = pool.getResource()) {
            for (long i = begin; i < begin + size; i++) {
                RawVPNResult r = null;
                try {
                    String json = redis.get(prefix + "vpn_values:" + i);
                    if (json == null) {
                        continue;
                    }
                    JSONObject obj = JSONUtil.parseObject(json);
                    r = new RawVPNResult(
                            i,
                            ((Number) obj.get("ipID")).longValue(),
                            obj.get("cascade") == null ? Optional.empty() : Optional.of((Boolean) obj.get("cascade")),
                            obj.get("consensus") == null ? Optional.empty() : Optional.of(((Number) obj.get("consensus")).doubleValue()),
                            ((Number) obj.get("created")).longValue()
                    );
                } catch (ParseException | ClassCastException ex) {
                    logger.warn("Could not get VPN data for ID " + i + ".", ex);
                }
                if (r != null) {
                    retVal.add(r);
                }
            }

            return retVal;
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public void loadVPNValues(Set<RawVPNResult> values, boolean truncate) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            if (truncate) {
                deleteNamespace(redis, prefix + "vpn_values:");
            }
            long max = 0;
            for (RawVPNResult r : values) {
                max = Math.max(max, r.getID());
                JSONObject obj = new JSONObject();
                obj.put("ipID", r.getIPID());
                obj.put("cascade", r.getCascade().orElse(null));
                obj.put("consensus", r.getConsensus().orElse(null));
                obj.put("created", r.getCreated());

                redis.set(prefix + "vpn_values:" + r.getID(), obj.toJSONString());

                obj.remove("ipID");
                obj.put("id", r.getID());
                redis.rpush(prefix + "vpn_values:ip:" + r.getIPID(), obj.toJSONString());
            }
            redis.set(prefix + "vpn_values:idx", String.valueOf(max));
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public Set<RawMCLeaksResult> dumpMCLeaksValues(long begin, int size) throws StorageException {
        Set<RawMCLeaksResult> retVal = new LinkedHashSet<>();

        try (Jedis redis = pool.getResource()) {
            for (long i = begin; i < begin + size; i++) {
                RawMCLeaksResult r = null;
                try {
                    String json = redis.get(prefix + "mcleaks_values:" + i);
                    if (json == null) {
                        continue;
                    }
                    JSONObject obj = JSONUtil.parseObject(json);
                    r = new RawMCLeaksResult(
                            i,
                            ((Number) obj.get("playerID")).longValue(),
                            (Boolean) obj.get("result"),
                            ((Number) obj.get("created")).longValue()
                    );
                } catch (ParseException | ClassCastException ex) {
                    logger.warn("Could not get MCLeaks data for ID " + i + ".", ex);
                }
                if (r != null) {
                    retVal.add(r);
                }
            }

            return retVal;
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    public void loadMCLeaksValues(Set<RawMCLeaksResult> values, boolean truncate) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            if (truncate) {
                deleteNamespace(redis, prefix + "mcleaks_values:");
            }
            long max = 0;
            for (RawMCLeaksResult r : values) {
                max = Math.max(max, r.getID());
                JSONObject obj = new JSONObject();
                obj.put("playerID", r.getLongPlayerID());
                obj.put("result", r.getValue());
                obj.put("created", r.getCreated());

                redis.set(prefix + "mcleaks_values:" + r.getID(), obj.toJSONString());

                obj.remove("playerID");
                obj.put("id", r.getID());
                redis.rpush(prefix + "mcleaks_values:player:" + r.getLongPlayerID(), obj.toJSONString());
            }
            redis.set(prefix + "mcleaks_values:idx", String.valueOf(max));
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    private VPNResult getVPNResult(long id, String json, Jedis redis) throws StorageException, JedisException, ParseException, ClassCastException {
        if (json == null) {
            return null;
        }

        JSONObject obj = JSONUtil.parseObject(json);
        long longIPID = ((Number) obj.get("ipID")).longValue();
        Optional<Boolean> cascade = obj.get("cascade") == null ? Optional.empty() : Optional.of((Boolean) obj.get("cascade"));
        Optional<Double> consensus = obj.get("consensus") == null ? Optional.empty() : Optional.of(((Number) obj.get("consensus")).doubleValue());
        long created = ((Number) obj.get("created")).longValue();

        String ipJSON = redis.get(prefix + "ips:" + longIPID);
        if (ipJSON == null) {
            throw new StorageException(false, "Could not get IP data for ID " + longIPID + ".");
        }
        JSONObject ipObj = JSONUtil.parseObject(ipJSON);
        String ip = (String) ipObj.get("ip");
        if (!ValidationUtil.isValidIp(ip)) {
            redis.del(prefix + "ips:" + longIPID);
            throw new StorageException(false, "IP ID " + longIPID + " has an invalid IP \"" + ip + "\".");
        }

        return new VPNResult(
                id,
                ip,
                cascade,
                consensus,
                created
        );
    }

    private VPNResult getVPNResultIP(long longIPID, String json, Jedis redis, long cacheTimeMillis) throws StorageException, JedisException, ParseException, ClassCastException {
        if (json == null) {
            return null;
        }

        JSONObject obj = JSONUtil.parseObject(json);
        long id = ((Number) obj.get("id")).longValue();
        Optional<Boolean> cascade = obj.get("cascade") == null ? Optional.empty() : Optional.of((Boolean) obj.get("cascade"));
        Optional<Double> consensus = obj.get("consensus") == null ? Optional.empty() : Optional.of(((Number) obj.get("consensus")).doubleValue());
        long created = ((Number) obj.get("created")).longValue();

        if (created < getTime(redis.time()) - cacheTimeMillis) {
            return null;
        }

        String ipJSON = redis.get(prefix + "ips:" + longIPID);
        if (ipJSON == null) {
            throw new StorageException(false, "Could not get IP data for ID " + longIPID + ".");
        }
        JSONObject ipObj = JSONUtil.parseObject(ipJSON);
        String ip = (String) ipObj.get("ip");
        if (!ValidationUtil.isValidIp(ip)) {
            redis.del(prefix + "ips:" + longIPID);
            throw new StorageException(false, "IP ID " + longIPID + " has an invalid IP \"" + ip + "\".");
        }

        return new VPNResult(
                id,
                ip,
                cascade,
                consensus,
                created
        );
    }

    private MCLeaksResult getMCLeaksResult(long id, String json, Jedis redis) throws StorageException, JedisException, ParseException, ClassCastException {
        if (json == null) {
            return null;
        }

        JSONObject obj = JSONUtil.parseObject(json);
        long longPlayerID = ((Number) obj.get("playerID")).longValue();
        boolean value = (Boolean) obj.get("result");
        long created = ((Number) obj.get("created")).longValue();

        String playerJSON = redis.get(prefix + "players:" + longPlayerID);
        if (playerJSON == null) {
            throw new StorageException(false, "Could not get player data for ID " + longPlayerID + ".");
        }
        JSONObject playerObj = JSONUtil.parseObject(playerJSON);
        String pid = (String) playerObj.get("id");
        if (!ValidationUtil.isValidUuid(pid)) {
            redis.del(prefix + "players:" + longPlayerID);
            throw new StorageException(false, "Player ID " + longPlayerID + " has an invalid UUID \"" + pid + "\".");
        }

        return new MCLeaksResult(
                id,
                UUID.fromString(pid),
                value,
                created
        );
    }

    private MCLeaksResult getMCLeaksResultPlayer(long longPlayerID, String json, Jedis redis, long cacheTimeMillis) throws StorageException, JedisException, ParseException, ClassCastException {
        if (json == null) {
            return null;
        }

        JSONObject obj = JSONUtil.parseObject(json);
        long id = ((Number) obj.get("id")).longValue();
        boolean value = (Boolean) obj.get("result");
        long created = ((Number) obj.get("created")).longValue();

        if (created < getTime(redis.time()) - cacheTimeMillis) {
            return null;
        }

        String playerJSON = redis.get(prefix + "players:" + longPlayerID);
        if (playerJSON == null) {
            throw new StorageException(false, "Could not get player data for ID " + longPlayerID + ".");
        }
        JSONObject playerObj = JSONUtil.parseObject(playerJSON);
        String pid = (String) playerObj.get("id");
        if (!ValidationUtil.isValidUuid(pid)) {
            redis.del(prefix + "players:" + longPlayerID);
            throw new StorageException(false, "Player ID " + longPlayerID + " has an invalid UUID \"" + pid + "\".");
        }

        return new MCLeaksResult(
                id,
                UUID.fromString(pid),
                value,
                created
        );
    }

    private void deleteNamespace(Jedis redis, String namespace) throws JedisException {
        long current = 0;
        ScanParams params = new ScanParams();
        params.match(namespace + "*");
        params.count(50);

        ScanResult<String> result;
        do {
            result = redis.scan(String.valueOf(current), params);
            List<String> r = result.getResult();
            if (!r.isEmpty()) {
                redis.del(r.toArray(new String[0]));
            }
            current = Long.parseLong(result.getCursor());
        } while (!result.isCompleteIteration());
    }

    private long getLongIPIDExpensive(String ip) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            // A majority of the time there'll be an ID
            String json = redis.get(prefix + "ips:" + ip);
            if (json != null) {
                JSONObject obj = null;
                try {
                    obj = JSONUtil.parseObject(json);
                } catch (ParseException | ClassCastException ex) {
                    redis.del(prefix + "ips:" + ip);
                    logger.warn("Could not parse IP data. Deleted key.");
                }
                if (obj != null) {
                    return ((Number) obj.get("longID")).longValue();
                }
            }

            // No ID, generate one
            JSONObject obj = new JSONObject();
            obj.put("ip", ip);

            JSONObject obj2 = new JSONObject();

            long id;
            do {
                do {
                    id = redis.incr(prefix + "ips:idx");
                } while (redis.exists(prefix + "ips:" + id));
                obj2.put("longID", id);
            } while (redis.msetnx(
                    prefix + "ips:" + id, obj.toJSONString(),
                    prefix + "ips:" + ip, obj2.toJSONString()
            ) == 0L);

            handler.ipIDCreationCallback(ip, id, this);
            return id;
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
    }

    private long getLongPlayerIDExpensive(UUID uuid) throws StorageException {
        try (Jedis redis = pool.getResource()) {
            // A majority of the time there'll be an ID
            String json = redis.get(prefix + "players:" + uuid.toString());
            if (json != null) {
                JSONObject obj = null;
                try {
                    obj = JSONUtil.parseObject(json);
                } catch (ParseException | ClassCastException ex) {
                    redis.del(prefix + "players:" + uuid.toString());
                    logger.warn("Could not parse player data. Deleted key.");
                }
                if (obj != null) {
                    return ((Number) obj.get("longID")).longValue();
                }
            }

            // No ID, generate one
            JSONObject obj = new JSONObject();
            obj.put("id", uuid.toString());

            JSONObject obj2 = new JSONObject();

            long id;
            do {
                do {
                    id = redis.incr(prefix + "players:idx");
                } while (redis.exists(prefix + "players:" + id));
                obj2.put("longID", id);
            } while (redis.msetnx(
                    prefix + "players:" + id, obj.toJSONString(),
                    prefix + "players:" + uuid.toString(), obj2.toJSONString()
            ) == 0L);

            handler.playerIDCreationCallback(uuid, id, this);
            return id;
        } catch (JedisException ex) {
            throw new StorageException(isAutomaticallyRecoverable(ex), ex);
        }
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

    // Redis returns a list
    // o[0] = unix time in seconds
    // o[1] = microseconds since last second
    // Therefore, to get unix time in millis we multiply seconds by 1000, divide microseconds by 1000, and add them together
    private long getTime(List<String> o) { return Long.parseLong(o.get(0)) * 1000L + Long.parseLong(o.get(1)) / 1000L; }
}

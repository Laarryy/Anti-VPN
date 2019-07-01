package me.egg82.antivpn.extended;

import com.google.common.collect.ImmutableSet;
import com.rabbitmq.client.ConnectionFactory;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.enums.SQLType;
import me.egg82.antivpn.services.InternalAPI;
import ninja.egg82.sql.SQL;
import ninja.egg82.tuples.longs.LongObjectPair;
import redis.clients.jedis.JedisPool;

public class CachedConfigValues {
    private CachedConfigValues() {}

    private ImmutableSet<String> sources = ImmutableSet.of();
    public ImmutableSet<String> getSources() { return sources; }

    private LongObjectPair<TimeUnit> sourceCacheTime = new LongObjectPair<>(6L, TimeUnit.HOURS);
    public long getSourceCacheTime() { return sourceCacheTime.getSecond().toMillis(sourceCacheTime.getFirst()); }

    private ImmutableSet<String> ignoredIps = ImmutableSet.of();
    public ImmutableSet<String> getIgnoredIps() { return ignoredIps; }

    private LongObjectPair<TimeUnit> cacheTime = new LongObjectPair<>(1L, TimeUnit.MINUTES);
    public long getCacheTime() { return cacheTime.getSecond().toMillis(cacheTime.getFirst()); }

    private boolean debug = false;
    public boolean getDebug() { return debug; }

    private int threads = 4;
    public int getThreads() { return threads; }

    private JedisPool redisPool = null;
    public JedisPool getRedisPool() { return redisPool; }

    private ConnectionFactory rabbitConnectionFactory = null;
    public ConnectionFactory getRabbitConnectionFactory() { return rabbitConnectionFactory; }

    private SQL sql = null;
    public SQL getSQL() { return sql; }

    private SQLType sqlType = SQLType.SQLite;
    public SQLType getSQLType() { return sqlType; }

    public static CachedConfigValues.Builder builder() { return new CachedConfigValues.Builder(); }

    public static class Builder {
        private final CachedConfigValues values = new CachedConfigValues();

        private Builder() {}

        public CachedConfigValues.Builder sources(Collection<String> value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.sources = ImmutableSet.copyOf(value);
            return this;
        }

        public CachedConfigValues.Builder sourceCacheTime(long value, TimeUnit unit) {
            if (value <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.sourceCacheTime = new LongObjectPair<>(value, unit);
            return this;
        }

        public CachedConfigValues.Builder ignoredIps(Collection<String> value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.ignoredIps = ImmutableSet.copyOf(value);
            return this;
        }

        public CachedConfigValues.Builder cacheTime(long value, TimeUnit unit) {
            if (value <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.cacheTime = new LongObjectPair<>(value, unit);
            return this;
        }

        public CachedConfigValues.Builder debug(boolean value) {
            values.debug = value;
            return this;
        }

        public CachedConfigValues.Builder threads(int value) {
            if (value <= 1) {
                throw new IllegalArgumentException("value cannot be <= 1.");
            }

            values.threads = value;
            return this;
        }

        public CachedConfigValues.Builder redisPool(JedisPool value) {
            values.redisPool = value;
            return this;
        }

        public CachedConfigValues.Builder rabbitConnectionFactory(ConnectionFactory value) {
            values.rabbitConnectionFactory = value;
            return this;
        }

        public CachedConfigValues.Builder sql(SQL value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.sql = value;
            return this;
        }

        public CachedConfigValues.Builder sqlType(String value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.sqlType = SQLType.getByName(value);
            return this;
        }

        public CachedConfigValues build() {
            InternalAPI.changeCacheTime(values.cacheTime.getFirst(), values.cacheTime.getSecond());
            return values;
        }
    }
}

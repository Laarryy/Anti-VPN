package me.egg82.antivpn.extended;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.apis.VPNAPI;
import me.egg82.antivpn.messaging.Messaging;
import me.egg82.antivpn.storage.Storage;

public class CachedConfigValues {
    private CachedConfigValues() {}

    private ImmutableList<Storage> storage = ImmutableList.of();
    public ImmutableList<Storage> getStorage() { return storage; }

    private ImmutableList<Messaging> messaging = ImmutableList.of();
    public ImmutableList<Messaging> getMessaging() { return messaging; }

    private ImmutableMap<String, VPNAPI> sources = ImmutableMap.of();
    public ImmutableMap<String, VPNAPI> getSources() { return sources; }

    private long sourceCacheTime = 21600000L; // 6 hours
    public long getSourceCacheTime() { return sourceCacheTime; }

    private long mcleaksCacheTime = 86400000L; // 1 day
    public long getMCLeaksCacheTime() { return mcleaksCacheTime; }

    private ImmutableSet<String> ignoredIps = ImmutableSet.of();
    public ImmutableSet<String> getIgnoredIps() { return ignoredIps; }

    private long cacheTime = 60000L; // 1 minute
    public long getCacheTime() { return cacheTime; }

    private boolean debug = false;
    public boolean getDebug() { return debug; }

    private int threads = 4;
    public int getThreads() { return threads; }

    private long timeout = 5000L;
    public long getTimeout() { return timeout; }

    private ImmutableList<String> vpnActionCommands = ImmutableList.of();
    public ImmutableList<String> getVPNActionCommands() { return vpnActionCommands; }

    private ImmutableList<String> mcleaksActionCommands = ImmutableList.of();
    public ImmutableList<String> getMCLeaksActionCommands() { return mcleaksActionCommands; }

    public static CachedConfigValues.Builder builder() { return new CachedConfigValues.Builder(); }

    public static class Builder {
        private final CachedConfigValues values = new CachedConfigValues();

        private Builder() {}

        public CachedConfigValues.Builder storage(List<Storage> value) {
            values.storage = ImmutableList.copyOf(value);
            return this;
        }

        public CachedConfigValues.Builder messaging(List<Messaging> value) {
            values.messaging = ImmutableList.copyOf(value);
            return this;
        }

        public CachedConfigValues.Builder sources(Map<String, VPNAPI> value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.sources = ImmutableMap.copyOf(value);
            return this;
        }

        public CachedConfigValues.Builder sourceCacheTime(long value, TimeUnit unit) {
            if (value <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.sourceCacheTime = unit.toMillis(value);
            return this;
        }

        public CachedConfigValues.Builder mcleaksCacheTime(long value, TimeUnit unit) {
            if (value <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.mcleaksCacheTime = unit.toMillis(value);
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

            values.cacheTime = unit.toMillis(value);
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

        public CachedConfigValues.Builder timeout(long value) {
            if (value <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.timeout = value;
            return this;
        }

        public CachedConfigValues.Builder vpnActionCommands(Collection<String> value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.vpnActionCommands = ImmutableList.copyOf(value);
            return this;
        }

        public CachedConfigValues.Builder mcleaksActionCommands(Collection<String> value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.mcleaksActionCommands = ImmutableList.copyOf(value);
            return this;
        }

        public CachedConfigValues build() {
            InternalAPI.changeCacheTime(values.cacheTime.getFirst(), values.cacheTime.getSecond());
            return values;
        }
    }
}

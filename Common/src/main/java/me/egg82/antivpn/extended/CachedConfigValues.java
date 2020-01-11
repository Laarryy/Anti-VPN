package me.egg82.antivpn.extended;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.apis.SourceAPI;
import me.egg82.antivpn.enums.VPNAlgorithmMethod;
import me.egg82.antivpn.messaging.Messaging;
import me.egg82.antivpn.storage.Storage;
import me.egg82.antivpn.utils.TimeUtil;

public class CachedConfigValues {
    private CachedConfigValues() {}

    private ImmutableList<Storage> storage = ImmutableList.of();
    public ImmutableList<Storage> getStorage() { return storage; }

    private ImmutableList<Messaging> messaging = ImmutableList.of();
    public ImmutableList<Messaging> getMessaging() { return messaging; }

    private ImmutableMap<String, SourceAPI> sources = ImmutableMap.of();
    public ImmutableMap<String, SourceAPI> getSources() { return sources; }

    private long sourceCacheTime = new TimeUtil.Time(6L, TimeUnit.HOURS).getMillis();
    public long getSourceCacheTime() { return sourceCacheTime; }

    private long mcleaksCacheTime = new TimeUtil.Time(1L, TimeUnit.DAYS).getMillis();
    public long getMCLeaksCacheTime() { return mcleaksCacheTime; }

    private ImmutableSet<String> ignoredIps = ImmutableSet.of();
    public ImmutableSet<String> getIgnoredIps() { return ignoredIps; }

    private TimeUtil.Time cacheTime = new TimeUtil.Time(1L, TimeUnit.MINUTES);
    public TimeUtil.Time getCacheTime() { return cacheTime; }

    private boolean debug = false;
    public boolean getDebug() { return debug; }

    private int threads = 4;
    public int getThreads() { return threads; }

    private long timeout = 5000L;
    public long getTimeout() { return timeout; }

    private String vpnKickMessage = "&cPlease disconnect from your proxy or VPN before re-joining!";
    public String getVPNKickMessage() { return vpnKickMessage; }

    private ImmutableList<String> vpnActionCommands = ImmutableList.of();
    public ImmutableList<String> getVPNActionCommands() { return vpnActionCommands; }

    private String mcleaksKickMessage = "&cPlease discontinue your use of an MCLeaks account!";
    public String getMCLeaksKickMessage() { return mcleaksKickMessage; }

    private ImmutableList<String> mcleaksActionCommands = ImmutableList.of();
    public ImmutableList<String> getMCLeaksActionCommands() { return mcleaksActionCommands; }

    private VPNAlgorithmMethod vpnAlgorithmMethod = VPNAlgorithmMethod.CASCADE;
    public VPNAlgorithmMethod getVPNAlgorithmMethod() { return vpnAlgorithmMethod; }

    private double vpnAlgorithmConsensus = 0.6d;
    public double getVPNAlgorithmConsensus() { return vpnAlgorithmConsensus; }

    public static CachedConfigValues.Builder builder() { return new CachedConfigValues.Builder(); }

    public static class Builder {
        private final CachedConfigValues values = new CachedConfigValues();

        private Builder() { }

        public CachedConfigValues.Builder debug(boolean value) {
            values.debug = value;
            return this;
        }

        public CachedConfigValues.Builder storage(List<Storage> value) {
            values.storage = ImmutableList.copyOf(value);
            return this;
        }

        public CachedConfigValues.Builder messaging(List<Messaging> value) {
            values.messaging = ImmutableList.copyOf(value);
            return this;
        }

        public CachedConfigValues.Builder sources(Map<String, SourceAPI> value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.sources = ImmutableMap.copyOf(value);
            return this;
        }

        public CachedConfigValues.Builder sourceCacheTime(TimeUtil.Time value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            if (value.getMillis() <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.sourceCacheTime = value.getMillis();
            return this;
        }

        public CachedConfigValues.Builder mcleaksCacheTime(TimeUtil.Time value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            if (value.getMillis() <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.mcleaksCacheTime = value.getMillis();
            return this;
        }

        public CachedConfigValues.Builder ignoredIps(Collection<String> value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.ignoredIps = ImmutableSet.copyOf(value);
            return this;
        }

        public CachedConfigValues.Builder cacheTime(TimeUtil.Time value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            if (value.getMillis() <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.cacheTime = value;
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

        public CachedConfigValues.Builder vpnKickMessage(String value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.vpnKickMessage = value;
            return this;
        }

        public CachedConfigValues.Builder vpnActionCommands(Collection<String> value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.vpnActionCommands = ImmutableList.copyOf(value);
            return this;
        }

        public CachedConfigValues.Builder mcleaksKickMessage(String value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.mcleaksKickMessage = value;
            return this;
        }

        public CachedConfigValues.Builder mcleaksActionCommands(Collection<String> value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.mcleaksActionCommands = ImmutableList.copyOf(value);
            return this;
        }

        public CachedConfigValues.Builder vpnAlgorithmMethod(VPNAlgorithmMethod value) {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null.");
            }
            values.vpnAlgorithmMethod = value;
            return this;
        }

        public CachedConfigValues.Builder vpnAlgorithmConsensus(double value) {
            if (value < 0.0d) {
                throw new IllegalArgumentException("value cannot be < 0.");
            }
            if (value > 1.0d) {
                throw new IllegalArgumentException("value cannot be > 1.");
            }
            values.vpnAlgorithmConsensus = value;
            return this;
        }

        public CachedConfigValues build() { return values; }
    }
}

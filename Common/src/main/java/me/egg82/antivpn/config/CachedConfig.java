package me.egg82.antivpn.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CachedConfig {
    private CachedConfig() { }

    private @NotNull ImmutableList<@NotNull StorageService> storage = ImmutableList.of();

    @NotNull
    public ImmutableList<@NotNull StorageService> getStorage() { return storage; }

    private @NotNull ImmutableList<@NotNull MessagingService> messaging = ImmutableList.of();

    @NotNull
    public ImmutableList<@NotNull MessagingService> getMessaging() { return messaging; }

    private long sourceCacheTime = new TimeUtil.Time(6L, TimeUnit.HOURS).getMillis();

    public long getSourceCacheTime() { return sourceCacheTime; }

    private long mcleaksCacheTime = new TimeUtil.Time(1L, TimeUnit.DAYS).getMillis();

    public long getMCLeaksCacheTime() { return mcleaksCacheTime; }

    private @NotNull ImmutableSet<@NotNull String> ignoredIps = ImmutableSet.of();

    @NotNull
    public ImmutableSet<@NotNull String> getIgnoredIps() { return ignoredIps; }

    private @NotNull TimeUtil.Time cacheTime = new TimeUtil.Time(1L, TimeUnit.MINUTES);

    @NotNull
    public TimeUtil.Time getCacheTime() { return cacheTime; }

    private boolean debug = false;

    public boolean getDebug() { return debug; }

    private @NotNull Locale language = Locale.US;

    @NotNull
    public Locale getLanguage() { return language; }

    private int threads = 4;

    public int getThreads() { return threads; }

    private long timeout = 5000L;

    public long getTimeout() { return timeout; }

    private @NotNull String vpnKickMessage = "<red>Please disconnect from your proxy or VPN before re-joining!</red>";

    @NotNull
    public String getVPNKickMessage() { return vpnKickMessage; }

    private @NotNull ImmutableList<@NotNull String> vpnActionCommands = ImmutableList.of();

    @NotNull
    public ImmutableList<@NotNull String> getVPNActionCommands() { return vpnActionCommands; }

    private @NotNull String mcleaksKickMessage = "<red>Please discontinue your use of an MCLeaks account!</red>";

    @NotNull
    public String getMCLeaksKickMessage() { return mcleaksKickMessage; }

    private @NotNull ImmutableList<@NotNull String> mcleaksActionCommands = ImmutableList.of();

    @NotNull
    public ImmutableList<@NotNull String> getMCLeaksActionCommands() { return mcleaksActionCommands; }

    private @NotNull AlgorithmMethod algorithmMethod = AlgorithmMethod.CASCADE;

    @NotNull
    public AlgorithmMethod getVPNAlgorithmMethod() { return algorithmMethod; }

    private double vpnAlgorithmConsensus = 0.6d;

    public double getVPNAlgorithmConsensus() { return vpnAlgorithmConsensus; }

    private @NotNull String mcleaksKey = "";

    @NotNull
    public String getMcLeaksKey() { return mcleaksKey; }

    private @NotNull String adminPermissionNode = "avpn.admin";

    @NotNull
    public String getAdminPermissionNode() { return adminPermissionNode; }

    private @NotNull String bypassPermissionNode = "avpn.bypass";

    @NotNull
    public String getBypassPermissionNode() { return bypassPermissionNode; }

    private @NotNull UUID serverId = UUID.randomUUID();

    @NotNull
    public UUID getServerId() { return serverId; }

    private @NotNull String serverIdString = serverId.toString();

    @NotNull
    public String getServerIdString() { return serverIdString; }

    @NotNull
    public static CachedConfig.Builder builder() { return new CachedConfig.Builder(); }

    public static class Builder {
        private final @NotNull CachedConfig values = new CachedConfig();

        private Builder() { }

        @NotNull
        public CachedConfig.Builder debug(boolean value) {
            values.debug = value;
            return this;
        }

        @NotNull
        public CachedConfig.Builder language(@NotNull Locale value) {
            values.language = value;
            return this;
        }

        @NotNull
        public CachedConfig.Builder storage(@NotNull List<@NotNull StorageService> value) {
            values.storage = ImmutableList.copyOf(value);
            return this;
        }

        @NotNull
        public CachedConfig.Builder messaging(@NotNull List<@NotNull MessagingService> value) {
            values.messaging = ImmutableList.copyOf(value);
            return this;
        }

        @NotNull
        public CachedConfig.Builder sourceCacheTime(@NotNull TimeUtil.Time value) {
            if (value.getMillis() <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.sourceCacheTime = value.getMillis();
            return this;
        }

        @NotNull
        public CachedConfig.Builder mcleaksCacheTime(@NotNull TimeUtil.Time value) {
            if (value.getMillis() <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.mcleaksCacheTime = value.getMillis();
            return this;
        }

        @NotNull
        public CachedConfig.Builder ignoredIps(@NotNull Collection<@NotNull String> value) {
            values.ignoredIps = ImmutableSet.copyOf(value);
            return this;
        }

        @NotNull
        public CachedConfig.Builder cacheTime(@NotNull TimeUtil.Time value) {
            if (value.getMillis() <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.cacheTime = value;
            return this;
        }

        @NotNull
        public CachedConfig.Builder threads(int value) {
            if (value <= 1) {
                throw new IllegalArgumentException("value cannot be <= 1.");
            }

            values.threads = value;
            return this;
        }

        @NotNull
        public CachedConfig.Builder timeout(long value) {
            if (value <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.timeout = value;
            return this;
        }

        @NotNull
        public CachedConfig.Builder vpnKickMessage(@NotNull String value) {
            values.vpnKickMessage = value;
            return this;
        }

        @NotNull
        public CachedConfig.Builder vpnActionCommands(@NotNull Collection<@NotNull String> value) {
            values.vpnActionCommands = ImmutableList.copyOf(value);
            return this;
        }

        @NotNull
        public CachedConfig.Builder mcleaksKickMessage(@NotNull String value) {
            values.mcleaksKickMessage = value;
            return this;
        }

        @NotNull
        public CachedConfig.Builder mcleaksActionCommands(@NotNull Collection<@NotNull String> value) {
            values.mcleaksActionCommands = ImmutableList.copyOf(value);
            return this;
        }

        @NotNull
        public CachedConfig.Builder vpnAlgorithmMethod(@NotNull AlgorithmMethod value) {
            values.algorithmMethod = value;
            return this;
        }

        @NotNull
        public CachedConfig.Builder vpnAlgorithmConsensus(double value) {
            if (value < 0.0d) {
                throw new IllegalArgumentException("value cannot be < 0.");
            }
            if (value > 1.0d) {
                throw new IllegalArgumentException("value cannot be > 1.");
            }
            values.vpnAlgorithmConsensus = value;
            return this;
        }

        @NotNull
        public CachedConfig.Builder mcleaksKey(@NotNull String value) {
            values.mcleaksKey = value;
            return this;
        }

        @NotNull
        public CachedConfig.Builder adminPermissionNode(@NotNull String value) {
            values.adminPermissionNode = value;
            return this;
        }

        @NotNull
        public CachedConfig.Builder bypassPermissionNode(@NotNull String value) {
            values.bypassPermissionNode = value;
            return this;
        }

        @NotNull
        public CachedConfig.Builder serverId(@NotNull UUID value) {
            values.serverId = value;
            values.serverIdString = value.toString();
            return this;
        }

        @NotNull
        public CachedConfig build() { return values; }
    }
}

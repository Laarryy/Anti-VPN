package me.egg82.antivpn.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;

public class CachedConfig {
    private CachedConfig() { }

    private ImmutableList<StorageService> storage = ImmutableList.of();

    public @NotNull ImmutableList<@NotNull StorageService> getStorage() { return storage; }

    private ImmutableList<MessagingService> messaging = ImmutableList.of();

    public @NotNull ImmutableList<@NotNull MessagingService> getMessaging() { return messaging; }

    private long sourceCacheTime = new TimeUtil.Time(6L, TimeUnit.HOURS).getMillis();

    public long getSourceCacheTime() { return sourceCacheTime; }

    private long mcleaksCacheTime = new TimeUtil.Time(1L, TimeUnit.DAYS).getMillis();

    public long getMCLeaksCacheTime() { return mcleaksCacheTime; }

    private ImmutableSet<String> ignoredIps = ImmutableSet.of();

    public @NotNull ImmutableSet<@NotNull String> getIgnoredIps() { return ignoredIps; }

    private TimeUtil.Time cacheTime = new TimeUtil.Time(1L, TimeUnit.MINUTES);

    public @NotNull TimeUtil.Time getCacheTime() { return cacheTime; }

    private boolean debug = false;

    public boolean getDebug() { return debug; }

    private Locale language = Locale.US;

    public @NotNull Locale getLanguage() { return language; }

    private int threads = 4;

    public int getThreads() { return threads; }

    private long timeout = 5000L;

    public long getTimeout() { return timeout; }

    private String vpnKickMessage = "<red>Please disconnect from your proxy or VPN before re-joining!</red>";

    public @NotNull String getVPNKickMessage() { return vpnKickMessage; }

    private ImmutableList<String> vpnActionCommands = ImmutableList.of();

    public @NotNull ImmutableList<@NotNull String> getVPNActionCommands() { return vpnActionCommands; }

    private String mcleaksKickMessage = "<red>Please discontinue your use of an MCLeaks account!</red>";

    public @NotNull String getMCLeaksKickMessage() { return mcleaksKickMessage; }

    private ImmutableList<String> mcleaksActionCommands = ImmutableList.of();

    public @NotNull ImmutableList<@NotNull String> getMCLeaksActionCommands() { return mcleaksActionCommands; }

    private AlgorithmMethod algorithmMethod = AlgorithmMethod.CASCADE;

    public @NotNull AlgorithmMethod getVPNAlgorithmMethod() { return algorithmMethod; }

    private double vpnAlgorithmConsensus = 0.6d;

    public double getVPNAlgorithmConsensus() { return vpnAlgorithmConsensus; }

    private String mcleaksKey = "";

    public @NotNull String getMcLeaksKey() { return mcleaksKey; }

    private String adminPermissionNode = "avpn.admin";

    public @NotNull String getAdminPermissionNode() { return adminPermissionNode; }

    private String bypassPermissionNode = "avpn.bypass";

    public @NotNull String getBypassPermissionNode() { return bypassPermissionNode; }

    private UUID serverId = UUID.randomUUID();

    public @NotNull UUID getServerId() { return serverId; }

    private String serverIdString = serverId.toString();

    public @NotNull String getServerIdString() { return serverIdString; }

    public static @NotNull CachedConfig.Builder builder() { return new CachedConfig.Builder(); }

    public static class Builder {
        private final CachedConfig values = new CachedConfig();

        private Builder() { }

        public @NotNull CachedConfig.Builder debug(boolean value) {
            values.debug = value;
            return this;
        }

        public @NotNull CachedConfig.Builder language(@NotNull Locale value) {
            values.language = value;
            return this;
        }

        public @NotNull CachedConfig.Builder storage(@NotNull List<@NotNull StorageService> value) {
            values.storage = ImmutableList.copyOf(value);
            return this;
        }

        public @NotNull CachedConfig.Builder messaging(@NotNull List<@NotNull MessagingService> value) {
            values.messaging = ImmutableList.copyOf(value);
            return this;
        }

        public @NotNull CachedConfig.Builder sourceCacheTime(@NotNull TimeUtil.Time value) {
            if (value.getMillis() <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.sourceCacheTime = value.getMillis();
            return this;
        }

        public @NotNull CachedConfig.Builder mcleaksCacheTime(@NotNull TimeUtil.Time value) {
            if (value.getMillis() <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.mcleaksCacheTime = value.getMillis();
            return this;
        }

        public @NotNull CachedConfig.Builder ignoredIps(@NotNull Collection<@NotNull String> value) {
            values.ignoredIps = ImmutableSet.copyOf(value);
            return this;
        }

        public @NotNull CachedConfig.Builder cacheTime(@NotNull TimeUtil.Time value) {
            if (value.getMillis() <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.cacheTime = value;
            return this;
        }

        public @NotNull CachedConfig.Builder threads(int value) {
            if (value <= 1) {
                throw new IllegalArgumentException("value cannot be <= 1.");
            }

            values.threads = value;
            return this;
        }

        public @NotNull CachedConfig.Builder timeout(long value) {
            if (value <= 0L) {
                throw new IllegalArgumentException("value cannot be <= 0.");
            }

            values.timeout = value;
            return this;
        }

        public @NotNull CachedConfig.Builder vpnKickMessage(@NotNull String value) {
            values.vpnKickMessage = value;
            return this;
        }

        public @NotNull CachedConfig.Builder vpnActionCommands(@NotNull Collection<@NotNull String> value) {
            values.vpnActionCommands = ImmutableList.copyOf(value);
            return this;
        }

        public @NotNull CachedConfig.Builder mcleaksKickMessage(@NotNull String value) {
            values.mcleaksKickMessage = value;
            return this;
        }

        public @NotNull CachedConfig.Builder mcleaksActionCommands(@NotNull Collection<@NotNull String> value) {
            values.mcleaksActionCommands = ImmutableList.copyOf(value);
            return this;
        }

        public @NotNull CachedConfig.Builder vpnAlgorithmMethod(@NotNull AlgorithmMethod value) {
            values.algorithmMethod = value;
            return this;
        }

        public @NotNull CachedConfig.Builder vpnAlgorithmConsensus(double value) {
            if (value < 0.0d) {
                throw new IllegalArgumentException("value cannot be < 0.");
            }
            if (value > 1.0d) {
                throw new IllegalArgumentException("value cannot be > 1.");
            }
            values.vpnAlgorithmConsensus = value;
            return this;
        }

        public @NotNull CachedConfig.Builder mcleaksKey(@NotNull String value) {
            values.mcleaksKey = value;
            return this;
        }

        public @NotNull CachedConfig.Builder adminPermissionNode(@NotNull String value) {
            values.adminPermissionNode = value;
            return this;
        }

        public @NotNull CachedConfig.Builder bypassPermissionNode(@NotNull String value) {
            values.bypassPermissionNode = value;
            return this;
        }

        public @NotNull CachedConfig.Builder serverId(@NotNull UUID value) {
            values.serverId = value;
            values.serverIdString = value.toString();
            return this;
        }

        public @NotNull CachedConfig build() { return values; }
    }
}

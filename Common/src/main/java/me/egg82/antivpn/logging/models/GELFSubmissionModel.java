package me.egg82.antivpn.logging.models;

import flexjson.JSON;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GELFSubmissionModel implements Serializable {
    private String version;
    private UUID host;
    @JSON(name = "short_message")
    private String shortMessage;
    @JSON(name = "full_message")
    private String fullMessage;
    private Instant timestamp;
    private int level;
    @JSON(name = "_plugin_version")
    private String pluginVersion;
    @JSON(name = "_platform")
    private String platform;
    @JSON(name = "_platform_version")
    private String platformVersion;

    public GELFSubmissionModel() {
        this.version = "1.1";
        this.host = new UUID(0L, 0L);
        this.shortMessage = "";
        this.fullMessage = null;
        this.timestamp = Instant.now();
        this.level = 3;
        this.pluginVersion = "";
        this.platform = "";
        this.platformVersion = "";
    }

    public @NotNull String getVersion() { return version; }

    public void setVersion(@NotNull String version) { this.version = version; }

    public @NotNull UUID getHost() { return host; }

    public void setHost(@NotNull UUID host) { this.host = host; }

    @JSON(name = "short_message")
    public @NotNull String getShortMessage() { return shortMessage; }

    @JSON(name = "short_message")
    public void setShortMessage(@NotNull String shortMessage) { this.shortMessage = shortMessage; }

    @JSON(name = "full_message")
    public @Nullable String getFullMessage() { return fullMessage; }

    @JSON(name = "full_message")
    public void setFullMessage(String fullMessage) { this.fullMessage = fullMessage; }

    public @NotNull Instant getTimestamp() { return timestamp; }

    public void setTimestamp(@NotNull Instant timestamp) { this.timestamp = timestamp; }

    public int getLevel() { return level; }

    public void setLevel(int level) { this.level = level; }

    @JSON(name = "_plugin_version")
    public @NotNull String getPluginVersion() { return pluginVersion; }

    @JSON(name = "_plugin_version")
    public void setPluginVersion(@NotNull String pluginVersion) { this.pluginVersion = pluginVersion; }

    @JSON(name = "_platform")
    public @NotNull String getPlatform() { return platform; }

    @JSON(name = "_platform")
    public void setPlatform(@NotNull String platform) { this.platform = platform; }

    @JSON(name = "_platform_version")
    public @NotNull String getPlatformVersion() { return platformVersion; }

    @JSON(name = "_platform_version")
    public void setPlatformVersion(@NotNull String platformVersion) { this.platformVersion = platformVersion; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GELFSubmissionModel)) return false;
        GELFSubmissionModel that = (GELFSubmissionModel) o;
        return level == that.level && version.equals(that.version) && host.equals(that.host) && shortMessage.equals(that.shortMessage) && Objects.equals(fullMessage, that.fullMessage) && timestamp.equals(that.timestamp) && pluginVersion.equals(that.pluginVersion) && platform.equals(that.platform) && platformVersion.equals(that.platformVersion);
    }

    public int hashCode() { return Objects.hash(version, host, shortMessage, fullMessage, timestamp, level, pluginVersion, platform, platformVersion); }

    public String toString() {
        return "GELFSubmissionModel{" +
            "version='" + version + '\'' +
            ", host=" + host +
            ", shortMessage='" + shortMessage + '\'' +
            ", fullMessage='" + fullMessage + '\'' +
            ", timestamp=" + timestamp +
            ", level=" + level +
            ", pluginVersion='" + pluginVersion + '\'' +
            ", platform='" + platform + '\'' +
            ", platformVersion='" + platformVersion + '\'' +
            '}';
    }
}

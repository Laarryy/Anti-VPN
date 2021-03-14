package me.egg82.antivpn.config;

import org.jetbrains.annotations.NotNull;

public class HiddenConfig {
    private HiddenConfig() { }

    private boolean packetDump = false;

    public boolean doPacketDump() { return packetDump; }

    public static @NotNull HiddenConfig.Builder builder() { return new Builder(); }

    public static class Builder {
        private final HiddenConfig values = new HiddenConfig();

        private Builder() { }

        public @NotNull HiddenConfig.Builder doPacketDump(boolean value) {
            values.packetDump = value;
            return this;
        }

        public @NotNull HiddenConfig build() { return values; }
    }
}

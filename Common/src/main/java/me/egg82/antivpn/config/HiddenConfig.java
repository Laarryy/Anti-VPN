package me.egg82.antivpn.config;

import org.jetbrains.annotations.NotNull;

public class HiddenConfig {
    private HiddenConfig() { }

    private boolean packetDump = false;

    public boolean doPacketDump() { return packetDump; }

    @NotNull
    public static HiddenConfig.Builder builder() { return new Builder(); }

    public static class Builder {
        private final @NotNull HiddenConfig values = new HiddenConfig();

        private Builder() { }

        @NotNull
        public HiddenConfig.Builder doPacketDump(boolean value) {
            values.packetDump = value;
            return this;
        }

        @NotNull
        public HiddenConfig build() { return values; }
    }
}

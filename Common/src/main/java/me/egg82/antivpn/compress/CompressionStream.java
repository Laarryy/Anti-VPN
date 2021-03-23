package me.egg82.antivpn.compress;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface CompressionStream {
    byte @NotNull [] compress(byte @NotNull [] buf) throws IOException;

    byte @NotNull [] decompress(byte @NotNull [] buf) throws IOException;
}

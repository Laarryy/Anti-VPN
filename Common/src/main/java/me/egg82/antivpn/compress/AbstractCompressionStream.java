package me.egg82.antivpn.compress;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public abstract class AbstractCompressionStream {
    public abstract byte @NotNull [] compress(byte @NotNull [] buf) throws IOException;

    public abstract byte @NotNull [] decompress(byte @NotNull [] buf) throws IOException;
}

package me.egg82.antivpn.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.jetbrains.annotations.NotNull;

public class GZIPCompressor {
    private GZIPCompressor() { }

    public static byte @NotNull [] compress(byte @NotNull [] buffer) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(buffer.length); GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(buffer);
            return out.toByteArray();
        }
    }

    public static byte @NotNull [] decompress(byte @NotNull [] buffer) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(buffer); GZIPInputStream gzip = new GZIPInputStream(in); ByteArrayOutputStream out = new ByteArrayOutputStream(buffer.length)) {
            byte[] buf = new byte[4096];
            int read;
            while ((read = gzip.read(buf)) > 0) {
                out.write(buf, 0, read);
            }
            return out.toByteArray();
        }
    }
}

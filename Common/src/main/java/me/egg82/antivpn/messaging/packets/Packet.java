package me.egg82.antivpn.messaging.packets;

import java.io.Serializable;
import java.nio.ByteBuffer;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface Packet extends Serializable {
    byte VERSION = 0x01;

    byte getPacketId();

    void read(@NonNull ByteBuffer buffer);
    void write(@NonNull ByteBuffer buffer);
}

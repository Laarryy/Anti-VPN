package me.egg82.antivpn.messaging.packets;

import java.io.Serializable;
import java.nio.ByteBuffer;

public interface Packet extends Serializable {
    byte VERSION = 0x01;

    byte getPacketId();

    void read(ByteBuffer buffer);
    void write(ByteBuffer buffer);
}

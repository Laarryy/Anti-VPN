package me.egg82.antivpn.messaging.packets;

import io.netty.buffer.ByteBuf;
import java.io.Serializable;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public interface Packet extends Serializable {
    byte VERSION = 0x02;

    byte getPacketId();

    void read(@NotNull ByteBuf buffer);

    void write(@NotNull ByteBuf buffer);

    @NotNull UUID getSender();
}

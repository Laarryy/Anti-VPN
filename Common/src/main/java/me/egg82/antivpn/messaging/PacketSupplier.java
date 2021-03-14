package me.egg82.antivpn.messaging;

import io.netty.buffer.ByteBuf;
import me.egg82.antivpn.messaging.packets.Packet;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface PacketSupplier<T extends Packet> {
    @NotNull T create(@NotNull UUID sender, @NotNull ByteBuf buffer);
}

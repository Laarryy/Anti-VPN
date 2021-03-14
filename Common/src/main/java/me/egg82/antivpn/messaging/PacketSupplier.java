package me.egg82.antivpn.messaging;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

import me.egg82.antivpn.messaging.packets.Packet;
import org.jetbrains.annotations.NotNull;

public interface PacketSupplier<T extends Packet> {
    @NotNull T create(@NotNull UUID sender, @NotNull ByteBuf buffer);
}

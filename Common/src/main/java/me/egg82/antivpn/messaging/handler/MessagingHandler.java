package me.egg82.antivpn.messaging.handler;

import me.egg82.antivpn.messaging.packets.Packet;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface MessagingHandler {
    void handlePacket(@NotNull UUID messageId, @NotNull String fromService, @NotNull Packet packet);
}

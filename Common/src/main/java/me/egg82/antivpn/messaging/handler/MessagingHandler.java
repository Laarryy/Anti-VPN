package me.egg82.antivpn.messaging.handler;

import java.util.UUID;
import me.egg82.antivpn.messaging.packets.Packet;
import org.jetbrains.annotations.NotNull;

public interface MessagingHandler {
    void handlePacket(@NotNull UUID messageId, @NotNull String fromService, @NotNull Packet packet);
}

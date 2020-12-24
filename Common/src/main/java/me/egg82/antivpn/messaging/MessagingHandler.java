package me.egg82.antivpn.messaging;

import java.util.UUID;
import me.egg82.antivpn.messaging.packets.Packet;

public interface MessagingHandler {
    void handlePacket(UUID messageId, Packet packet);

    void cancel();
}

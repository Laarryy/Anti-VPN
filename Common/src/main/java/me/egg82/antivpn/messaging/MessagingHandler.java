package me.egg82.antivpn.messaging;

import java.util.UUID;
import me.egg82.antivpn.messaging.packets.Packet;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface MessagingHandler {
    void handlePacket(@NonNull UUID messageId, @NonNull String fromService, @NonNull Packet packet);

    void cancel();
}

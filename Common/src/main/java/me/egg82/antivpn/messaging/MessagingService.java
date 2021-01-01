package me.egg82.antivpn.messaging;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import me.egg82.antivpn.messaging.packets.Packet;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface MessagingService {
    String getName();

    void close();
    boolean isClosed();

    void sendPacket(@NonNull UUID messageId, @NonNull Packet packet) throws IOException, TimeoutException;
}

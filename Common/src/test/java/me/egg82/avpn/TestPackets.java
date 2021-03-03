package me.egg82.avpn;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.reflect.PackageFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestPackets {
    @Test
    void ensureCorrectPacketConstructors() {
        List<Class<Packet>> packetClasses = PackageFilter.getClasses(Packet.class, "me.egg82.avpn.messaging.packets", false, false, false);
        for (Class<Packet> clazz : packetClasses) {
            Assertions.assertDoesNotThrow(() -> { clazz.getConstructor(); });
            Assertions.assertDoesNotThrow(() -> { clazz.getConstructor(UUID.class, ByteBuf.class); });
        }
    }
}

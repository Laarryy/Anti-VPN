package me.egg82.antivpn.messaging;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import me.egg82.antivpn.messaging.packets.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PacketManager {
    private static final Set<Class<? extends Packet>> registeredPackets = new CopyOnWriteArraySet<>();
    private static final Int2ObjectMap<Class<? extends Packet>> packetsById = new Int2ObjectArrayMap<>();
    private static final Object2IntMap<Class<? extends Packet>> packets = new Object2IntArrayMap<>();
    private static final Int2ObjectMap<PacketSupplier<? extends Packet>> suppliersById = new Int2ObjectArrayMap<>();
    private static final AtomicInteger currentId = new AtomicInteger(0);

    private PacketManager() { }

    public static <T extends Packet> void register(@NotNull Class<T> clazz, @NotNull PacketSupplier<T> supplier) {
        if (!registeredPackets.add(clazz)) {
            throw new IllegalStateException("Packet " + clazz.getName() + " has already been registered.");
        }

        int id = currentId.getAndIncrement();
        packetsById.put(id, clazz);
        packets.put(clazz, id);
        suppliersById.put(id, supplier);
    }

    public static @Nullable Class<? extends Packet> getPacket(int id) { return packetsById.get(id); }

    public static int getId(Class<? extends Packet> clazz) { return packets.getOrDefault(clazz, -1); }

    public static <T extends Packet> @Nullable T read(int id, @NotNull UUID sender, @NotNull ByteBuf buffer) {
        PacketSupplier<? extends Packet> retVal = suppliersById.get(id);
        return retVal != null ? (T) retVal.create(sender, buffer) : null;
    }
}

package me.egg82.antivpn.messaging.handler;

import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.messaging.packets.Packet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMessagingHandler {
    protected final Logger logger = new GELFLogger(LoggerFactory.getLogger(getClass()));

    protected abstract boolean handlePacket(@NotNull Packet packet);
}

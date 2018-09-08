package me.egg82.avpn.messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import me.egg82.avpn.utils.IPCacheUtil;
import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.handlers.async.AsyncMessageHandler;
import ninja.egg82.plugin.messaging.IMessageHandler;

public class IPMessage extends AsyncMessageHandler {
    // vars

    // constructor
    public IPMessage() {
        super();
    }

    // public

    // private
    @SuppressWarnings("resource")
    protected void onExecute(long elapsedMilliseconds) {
        if (!channelName.equals("avpn-ip")) {
            return;
        }

        IMessageHandler messageHandler = ServiceLocator.getService(IMessageHandler.class);
        if (this.getSender().equals(messageHandler.getSenderId())) {
            return;
        }

        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(stream);

        String ip = null;
        boolean value = false;
        long created = -1L;

        try {
            ip = in.readUTF();
            value = in.readBoolean();
            created = in.readLong();
        } catch (Exception ex) {
            IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
            if (handler != null) {
                handler.sendException(ex);
            }
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }

        IPCacheUtil.addToCache(ip, value, created, true);
    }
}

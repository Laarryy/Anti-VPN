package me.egg82.avpn.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.messaging.IMessageHandler;
import ninja.egg82.plugin.utils.ChannelUtil;

public class IPChannelUtil {
    // vars

    // constructor
    public IPChannelUtil() {

    }

    // public
    public static void broadcastInfo(String ip, double value, long created) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (ServiceLocator.getService(IMessageHandler.class) == null) {
            return;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);

        try {
            out.writeUTF(ip);
            out.writeDouble(value);
            out.writeLong(created);
        } catch (Exception ex) {
            IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
            if (handler != null) {
                handler.sendException(ex);
            }
            ex.printStackTrace();
            return;
        }

        ChannelUtil.broadcastToProxies("avpn-consensus", stream.toByteArray());
        ChannelUtil.broadcastToServers("avpn-consensus", stream.toByteArray());
    }

    public static void broadcastInfo(String ip, boolean value, long created) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (ServiceLocator.getService(IMessageHandler.class) == null) {
            return;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);

        try {
            out.writeUTF(ip);
            out.writeBoolean(value);
            out.writeLong(created);
        } catch (Exception ex) {
            IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
            if (handler != null) {
                handler.sendException(ex);
            }
            ex.printStackTrace();
            return;
        }

        ChannelUtil.broadcastToProxies("avpn-ip", stream.toByteArray());
        ChannelUtil.broadcastToServers("avpn-ip", stream.toByteArray());
    }

    // private

}

package me.egg82.avpn.events;

import java.util.List;

import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;

import net.kyori.text.format.TextColor;
import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.handlers.events.async.AsyncEventHandler;
import ninja.egg82.plugin.messaging.IMessageHandler;
import ninja.egg82.sql.ISQL;
import ninja.egg82.utils.ThreadUtil;
import ninja.egg82.velocity.BasePlugin;
import ninja.egg82.velocity.processors.CommandProcessor;
import ninja.egg82.velocity.processors.EventProcessor;
import redis.clients.jedis.JedisPool;

public class ProxyShutdown extends AsyncEventHandler<ProxyShutdownEvent> {
    // vars

    // constructor
    public ProxyShutdown() {

    }

    // public

    // private
    @SuppressWarnings("resource")
    protected void onExecute(long elapsedMilliseconds) {
        ThreadUtil.shutdown(1000L);

        List<ISQL> sqls = ServiceLocator.removeServices(ISQL.class);
        for (ISQL sql : sqls) {
            sql.disconnect();
        }

        JedisPool jedisPool = ServiceLocator.getService(JedisPool.class);
        if (jedisPool != null) {
            jedisPool.close();
        }

        List<IMessageHandler> services = ServiceLocator.removeServices(IMessageHandler.class);
        for (IMessageHandler handler : services) {
            try {
                handler.close();
            } catch (Exception ex) {

            }
        }

        ServiceLocator.getService(CommandProcessor.class).clear();
        ServiceLocator.getService(EventProcessor.class).clear();

        ServiceLocator.getService(IExceptionHandler.class).close();

        ServiceLocator.getService(BasePlugin.class).printInfo("Disabled", TextColor.RED);
    }
}

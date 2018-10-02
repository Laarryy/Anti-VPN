package me.egg82.avpn;

import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.enums.SenderType;
import ninja.egg82.plugin.messaging.IMessageHandler;
import ninja.egg82.plugin.messaging.RabbitMessageHandler;
import ninja.egg82.velocity.BasePlugin;
import ninja.egg82.velocity.messaging.EnhancedVelocityMessageHandler;

public class MessagingLoader {
    // vars

    // constructor
    public MessagingLoader() {

    }

    // public
    @SuppressWarnings("resource")
    public static void loadMessaging(String pluginName, String serverName, String serverId, SenderType senderType) {
        if (pluginName == null) {
            throw new IllegalArgumentException("pluginName cannot be null.");
        }
        if (serverId == null) {
            throw new IllegalArgumentException("serverId cannot be null.");
        }
        if (serverId.isEmpty()) {
            throw new IllegalArgumentException("serverId cannot be empty.");
        }

        if (serverName == null || serverName.isEmpty() || serverName.equalsIgnoreCase("unknown") || serverName.equalsIgnoreCase("unknown server") || serverName.equalsIgnoreCase("unconfigured")
            || serverName.equalsIgnoreCase("unnamed") || serverName.equalsIgnoreCase("default")) {
            serverName = null;
        }

        Configuration config = ServiceLocator.getService(Configuration.class);

        String type = config.getNode("messaging", "type").getString("bungee");

        if (type.equalsIgnoreCase("default") || type.equalsIgnoreCase("bungee") || type.equalsIgnoreCase("bungeecord")) {
            if (!ServiceLocator.hasService(EnhancedVelocityMessageHandler.class)) {
                ServiceLocator.provideService(new EnhancedVelocityMessageHandler(pluginName, (serverName != null) ? serverName : serverId));
            }
        } else if (type.equalsIgnoreCase("rabbit") || type.equalsIgnoreCase("rabbitmq")) {
            ServiceLocator.provideService(new RabbitMessageHandler(config.getNode("messaging", "rabbit", "address").getString("127.0.0.1"), config.getNode("messaging", "rabbit", "port").getInt(5672),
                config.getNode("messaging", "rabbit", "user").getString("guest"), config.getNode("messaging", "rabbit", "pass").getString("guest"), pluginName,
                (serverName != null) ? serverName : serverId, senderType));
        } else {
            ServiceLocator.getService(BasePlugin.class).printWarning("Config \"messaging.type\" does not match expected values. Using Bungeecord default.");
            if (!ServiceLocator.hasService(EnhancedVelocityMessageHandler.class)) {
                ServiceLocator.provideService(new EnhancedVelocityMessageHandler(pluginName, (serverName != null) ? serverName : serverId));
            }
        }

        ServiceLocator.getService(IMessageHandler.class).createChannel("avpn-ip");
        ServiceLocator.getService(IMessageHandler.class).createChannel("avpn-consensus");
    }

    // private

}

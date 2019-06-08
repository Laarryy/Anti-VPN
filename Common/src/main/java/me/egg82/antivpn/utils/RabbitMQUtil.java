package me.egg82.antivpn.utils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import me.egg82.antivpn.extended.CachedConfigValues;

public class RabbitMQUtil {
    public static Connection getConnection() throws IOException, TimeoutException {
        Optional<CachedConfigValues> configValues = ConfigUtil.getCachedConfig();
        if (!configValues.isPresent()) {
            return null;
        }

        Connection connection = null;
        if (configValues.get().getRabbitConnectionFactory() != null) {
            connection = configValues.get().getRabbitConnectionFactory().newConnection();
        }
        return connection;
    }

    public static Channel getChannel(Connection connection) throws IOException {
        Channel retVal = null;
        if (connection != null) {
            retVal = connection.createChannel();
        }
        return retVal;
    }
}

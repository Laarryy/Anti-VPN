package me.egg82.antivpn.utils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQUtil {
    public static Connection getConnection(ConnectionFactory factory) throws IOException, TimeoutException {
        Connection retVal = null;
        if (factory != null) {
            retVal = factory.newConnection();
        }
        return retVal;
    }

    public static Channel getChannel(Connection connection) throws IOException {
        Channel retVal = null;
        if (connection != null) {
            retVal = connection.createChannel();
        }
        return retVal;
    }
}

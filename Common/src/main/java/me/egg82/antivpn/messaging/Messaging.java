package me.egg82.antivpn.messaging;

import java.util.Optional;
import java.util.UUID;

public interface Messaging {
    void close();
    boolean isClosed();

    void sendIP(UUID messageID, long longIPID, String ip) throws MessagingException;
    void sendPlayer(UUID messageID, long longPlayerID, UUID playerID) throws MessagingException;
    void sendPostVPN(UUID messageID, long id, long longIPID, String ip, Optional<Boolean> cascade, Optional<Double> consensus, long created) throws MessagingException;
    void sendPostMCLeaks(UUID messageID, long id, long longPlayerID, UUID playerID, boolean value, long created) throws MessagingException;
}

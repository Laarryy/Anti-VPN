package me.egg82.antivpn.services;

import me.egg82.antivpn.messaging.Messaging;

import java.util.Optional;
import java.util.UUID;

public interface MessagingHandler {
    void ipCallback(UUID messageID, String ip, long longIPID, Messaging callingMessaging);
    void playerCallback(UUID messageID, UUID playerID, long longPlayerID, Messaging callingMessaging);
    void postVPNCallback(UUID messageID, long id, long longIPID, String ip, Optional<Boolean> cascade, Optional<Double> consensus, long created, Messaging callingMessaging);
    void postMCLeaksCallback(UUID messageID, long id, long longPlayerID, UUID playerID, boolean value, long created, Messaging callingMessaging);
}

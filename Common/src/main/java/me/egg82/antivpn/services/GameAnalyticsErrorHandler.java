package me.egg82.antivpn.services;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import me.egg82.antivpn.config.ServiceKeys;
import ninja.egg82.analytics.GameAnalytics;
import ninja.egg82.analytics.common.Severity;
import ninja.egg82.analytics.events.GAError;
import ninja.egg82.analytics.events.GASessionEnd;
import ninja.egg82.analytics.events.GASessionStart;
import ninja.egg82.analytics.events.base.GAEventBase;
import org.json.simple.parser.ParseException;

public class GameAnalyticsErrorHandler {
    private static GameAnalytics gameAnalytics = null;
    private static GAEventBase eventBase = null;
    private static long start = -1L;

    private GameAnalyticsErrorHandler() {}

    public static void sendException(Throwable exception) {
        try {
            gameAnalytics.queueEvent(GAError.builder(eventBase, exception).build());
        } catch (IOException ex) {
            if (!ex.getMessage().equals("Connection has been closed.")) {
                ex.printStackTrace();
            }
        }
    }

    public static void sendMessage(Severity severity, String message) {
        if (gameAnalytics == null) {
            return;
        }

        try {
            gameAnalytics.queueEvent(GAError.builder(eventBase, severity, message).build());
        } catch (IOException ex) {
            if (!ex.getMessage().equals("Connection has been closed.")) {
                ex.printStackTrace();
            }
        }
    }

    public static void open(UUID userID, String buildVersion, String serverVersion) {
        try {
            gameAnalytics = new GameAnalytics(ServiceKeys.GAMEANALYTICS_KEY, ServiceKeys.GAMEANALYTICS_SECRET, 5);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | ParseException ex) {
            ex.printStackTrace();
            return;
        }

        eventBase = GAEventBase.builder(gameAnalytics, userID, 1).buildVersion(buildVersion).engineVersion(serverVersion).build();

        start = System.currentTimeMillis();
        try {
            gameAnalytics.queueEvent(GASessionStart.builder(eventBase).build());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void close() {
        if (gameAnalytics == null) {
            return;
        }

        try {
            gameAnalytics.queueEvent(GASessionEnd.builder(eventBase, Math.floorDiv(System.currentTimeMillis() - start, 1000L)).build());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        gameAnalytics.close();
    }
}

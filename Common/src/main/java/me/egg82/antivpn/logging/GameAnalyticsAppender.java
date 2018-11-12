package me.egg82.antivpn.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.services.GameAnalyticsErrorHandler;
import ninja.egg82.analytics.common.Severity;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;

public class GameAnalyticsAppender extends AppenderBase<ILoggingEvent> {
    protected void append(ILoggingEvent event) {
        if (event.getLevel() != Level.ERROR && event.getLevel() != Level.WARN) {
            return;
        }

        Configuration config;
        try {
            config = ServiceLocator.get(Configuration.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            ex.printStackTrace();
            return;
        }

        if (!config.getNode("stats", "errors").getBoolean(true)) {
            return;
        }

        Severity severity = getSeverity(event.getLevel());
        String message;

        if (event.getThrowableProxy() != null) {
            message = event.getThrowableProxy().getMessage();
        } else if (event.getMessage() != null) {
            message = event.getMessage();
        } else {
            new IllegalArgumentException("event does not have a thrown or a message.").printStackTrace();
            return;
        }

        GameAnalyticsErrorHandler.sendMessage(severity, message);
    }

    private Severity getSeverity(Level level) {
        if (level == Level.WARN) {
            return Severity.WARNING;
        } else if (level == Level.INFO) {
            return Severity.INFO;
        } else if (level == Level.DEBUG || level == Level.TRACE) {
            return Severity.DEBUG;
        } else {
            return Severity.ERROR;
        }
    }
}

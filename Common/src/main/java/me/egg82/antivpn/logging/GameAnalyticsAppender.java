package me.egg82.antivpn.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class GameAnalyticsAppender extends AppenderBase<ILoggingEvent> {
    //TODO: Figure out why this doesn't seem to work (?) and finish

    protected void append(ILoggingEvent event) {
        if (event.getLevel() != Level.ERROR && event.getLevel() != Level.WARN) {
            //System.out.println("GameAnalytics handler (tossed): " + event.getMessage());
            return;
        }

        //System.out.println("GameAnalytics handler: " + event.getMessage());
    }
}

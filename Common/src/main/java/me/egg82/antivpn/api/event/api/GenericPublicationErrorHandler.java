package me.egg82.antivpn.api.event.api;

import me.egg82.antivpn.config.ConfigUtil;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.bus.error.PublicationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericPublicationErrorHandler implements IPublicationErrorHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public GenericPublicationErrorHandler() { }

    public void handleError(PublicationError error) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.error("[Anti-VPN API] " + error.getMessage(), error.getCause());
        } else {
            logger.error("[Anti-VPN API] " + error.getMessage());
        }
        logger.warn("[Anti-VPN API] The above error is from a different plugin. PLEASE DO NOT REPORT THIS TO ANTI-VPN.");
    }
}

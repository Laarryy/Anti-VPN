package me.egg82.antivpn.api.event.api;

import me.egg82.antivpn.lang.I18NManager;
import me.egg82.antivpn.lang.MessageKey;
import me.egg82.antivpn.logging.GELFLogger;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.bus.error.PublicationError;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericPublicationErrorHandler implements IPublicationErrorHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final I18NManager consoleLocalizationManager;

    public GenericPublicationErrorHandler(@NotNull I18NManager consoleLocalizationManager) {
        this.consoleLocalizationManager = consoleLocalizationManager;
    }

    public void handleError(@NotNull PublicationError error) {
        String decorator = consoleLocalizationManager.getText(MessageKey.API__DECORATOR);
        GELFLogger.exception(logger, error.getCause(), consoleLocalizationManager, MessageKey.API__EVENT_ERROR, "{decorator}", decorator, "{message}", error.getMessage());
        logger.warn(consoleLocalizationManager.getText(MessageKey.API__NO_REPORT, "{decorator}", decorator));
    }
}

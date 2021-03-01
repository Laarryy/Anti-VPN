package me.egg82.antivpn.api;

import me.egg82.antivpn.lang.LocaleUtil;
import me.egg82.antivpn.lang.MessageKey;
import me.egg82.antivpn.logging.GELFLogger;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.bus.error.PublicationError;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericPublicationErrorHandler implements IPublicationErrorHandler {
    private final Logger logger = new GELFLogger(LoggerFactory.getLogger(getClass()));

    public GenericPublicationErrorHandler() { }

    public void handleError(@NotNull PublicationError error) {
        String decorator = LocaleUtil.getDefaultI18N().getText(MessageKey.API__DECORATOR);
        logger.error(LocaleUtil.getDefaultI18N().getText(MessageKey.API__EVENT_ERROR, "{decorator}", decorator, "{message}", error.getMessage()), error.getCause());
        logger.warn(LocaleUtil.getDefaultI18N().getText(MessageKey.API__NO_REPORT, "{decorator}", decorator));
    }
}

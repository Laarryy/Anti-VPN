package me.egg82.antivpn.utils;

import java.util.Map;
import me.egg82.antivpn.api.event.VPNEvent;
import me.egg82.antivpn.locale.LocaleUtil;
import me.egg82.antivpn.locale.MessageKey;
import net.kyori.event.EventBus;
import net.kyori.event.EventSubscriber;
import net.kyori.event.PostResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventUtil {
    private static final Logger logger = LoggerFactory.getLogger(EventUtil.class);

    private EventUtil() { }

    public static void post(@NotNull VPNEvent event, @NotNull EventBus<VPNEvent> eventBus) {
        PostResult result = eventBus.post(event);
        if (!result.wasSuccessful()) {
            String decorator = LocaleUtil.getDefaultI18N().getText(MessageKey.API__DECORATOR);

            for (Map.Entry<EventSubscriber<?>, Throwable> kvp : result.exceptions().entrySet()) {
                logger.error(LocaleUtil.getDefaultI18N().getText(MessageKey.API__EVENT_ERROR, "{decorator}", decorator, "{message}", kvp.getValue().getMessage()), kvp.getValue());
            }
            logger.warn(LocaleUtil.getDefaultI18N().getText(MessageKey.API__NO_REPORT, "{decorator}", decorator));
        }
    }
}

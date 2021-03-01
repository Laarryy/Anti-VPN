package me.egg82.antivpn.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import me.egg82.antivpn.logging.GELFLogger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassFilter {
    private static final Logger logger = new GELFLogger(LoggerFactory.getLogger(ClassFilter.class));

    private ClassFilter() { }

    public static @NotNull Object @NotNull [] getStaticFields(@NotNull Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        List<Object> returns = new ArrayList<>();

        for (Field field : fields) {
            if (!Modifier.isPrivate(field.getModifiers())) {
                try {
                    returns.add(field.get(null));
                } catch (IllegalAccessException ex) {
                    logger.warn(ex.getMessage(), ex);
                }
            }
        }

        return returns.toArray();
    }
}

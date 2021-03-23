package me.egg82.antivpn.json.transformers;

import flexjson.ObjectBinder;
import flexjson.ObjectFactory;
import flexjson.transformer.AbstractTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.time.Instant;

public class InstantTransformer extends AbstractTransformer implements ObjectFactory {
    @Override
    @Nullable
    public Object instantiate(@NotNull ObjectBinder context, @Nullable Object value, @NotNull Type targetType, @NotNull Class targetClass) {
        if (value instanceof Instant) {
            return value.toString();
        } else if (value instanceof CharSequence) {
            return Instant.parse((CharSequence) value);
        } else if (value instanceof Number) {
            return Instant.ofEpochSecond(((Number) value).longValue());
        }
        return null;
    }

    @Override
    public void transform(@Nullable Object object) {
        if (object != null) {
            getContext().writeQuoted(object.toString());
        } else {
            getContext().write("null");
        }
    }
}

package me.egg82.antivpn.web.transformers;

import flexjson.ObjectBinder;
import flexjson.ObjectFactory;
import flexjson.transformer.AbstractTransformer;
import java.lang.reflect.Type;
import java.time.Instant;

public class InstantTransformer extends AbstractTransformer implements ObjectFactory {
    public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
        if (value instanceof Instant) {
            return value.toString();
        } else if (value instanceof CharSequence) {
            return Instant.parse((CharSequence) value);
        } else if (value instanceof Number) {
            return Instant.ofEpochSecond(((Number) value).longValue());
        }
        return null;
    }

    public void transform(Object object) {
        if (object != null) {
            getContext().writeQuoted(object.toString());
        } else {
            getContext().write("null");
        }
    }
}

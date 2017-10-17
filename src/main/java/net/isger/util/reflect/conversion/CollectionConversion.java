package net.isger.util.reflect.conversion;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

import net.isger.util.Reflects;
import net.isger.util.reflect.Converter;

public class CollectionConversion implements Conversion {

    public static final CollectionConversion CONVERSION = new CollectionConversion();

    private CollectionConversion() {
    }

    public boolean isSupport(Type type) {
        return Collection.class.isAssignableFrom(Reflects.getRawClass(type));
    }

    @SuppressWarnings("unchecked")
    public Object convert(Type type, Object value) {
        if (value instanceof Object[]) {
            value = Arrays.asList((Object[]) value);
        } else if (!(value instanceof Collection)) {
            value = Arrays.asList(value);
        }
        Collection<Object> result = (Collection<Object>) Reflects
                .newInstance(Reflects.getRawClass(type));
        Class<?> actualClass = (Class<?>) Reflects.getActualType(type);
        for (Object instance : (Collection<?>) value) {
            result.add(Converter.convert(actualClass, instance));
        }
        return result;
    }

    public String toString() {
        return "collection";
    }

}

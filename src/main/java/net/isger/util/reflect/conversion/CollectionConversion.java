package net.isger.util.reflect.conversion;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;

import net.isger.util.reflect.Converter;

public class CollectionConversion implements Conversion {

    public static final CollectionConversion CONVERSION = new CollectionConversion();

    private CollectionConversion() {
    }

    public boolean isSupport(Class<?> type) {
        return type.equals(Collection.class);
    }

    public Object convert(Class<?> type, Object value) {
        if (value instanceof Object[]) {
            value = Arrays.asList((Object[]) value);
        } else if (!(value instanceof Collection)) {
            throw new IllegalStateException("Unexpected class conversion for "
                    + value);
        }
        Class<?> componentType = type.getComponentType();
        if (componentType.isAssignableFrom(value.getClass().getComponentType())) {
            return value;
        }
        int count = ((Collection<?>) value).size();
        Object[] values = ((Collection<?>) value).toArray();
        Object array = Array.newInstance(componentType, count);
        for (int i = 0; i < count; i++) {
            Array.set(array, i, Converter.convert(componentType, values[i]));
        }
        return Arrays.asList((Object[]) array);
    }

    public String toString() {
        return Collection.class.getName();
    }

}

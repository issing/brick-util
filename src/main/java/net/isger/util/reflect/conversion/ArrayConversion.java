package net.isger.util.reflect.conversion;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

import com.google.gson.JsonElement;

import net.isger.util.Reflects;
import net.isger.util.reflect.ClassAssembler;
import net.isger.util.reflect.Converter;

public class ArrayConversion implements Conversion {

    public static final ArrayConversion CONVERSION = new ArrayConversion();

    private ArrayConversion() {
    }

    public boolean isSupport(Type type) {
        return Reflects.getRawClass(type).isArray();
    }

    public Object convert(Type type, Object value, ClassAssembler assembler) {
        if (value instanceof Object[]) {
            value = Arrays.asList((Object[]) value);
        } else if (value instanceof JsonElement) {
            value = ((JsonElement) value).getAsJsonArray().asList();
        } else if (!(value instanceof Collection)) {
            value = Arrays.asList(value);
        }
        Class<?> componentClass = Reflects.getRawClass(type).getComponentType();
        int count = ((Collection<?>) value).size();
        Object[] values = ((Collection<?>) value).toArray();
        Object array = Array.newInstance(componentClass, count);
        for (int i = 0; i < count; i++) {
            Array.set(array, i, Converter.convert(componentClass, values[i], assembler));
        }
        return array;
    }

    public String toString() {
        return "array";
    }

}

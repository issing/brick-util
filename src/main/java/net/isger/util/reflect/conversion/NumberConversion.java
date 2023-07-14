package net.isger.util.reflect.conversion;

import java.lang.reflect.Type;

import net.isger.util.Reflects;
import net.isger.util.Strings;
import net.isger.util.reflect.ClassAssembler;
import net.isger.util.reflect.Converter;

public class NumberConversion implements Conversion {

    public static final NumberConversion CONVERSION = new NumberConversion();

    private NumberConversion() {
    }

    public boolean isSupport(Type type) {
        Class<?> rawClass = Reflects.getRawClass(type);
        return Boolean.class.isAssignableFrom(rawClass) || Character.class.isAssignableFrom(rawClass) || Number.class.isAssignableFrom(rawClass) || rawClass.isPrimitive();
    }

    public Object convert(Type type, Object value, ClassAssembler assembler) {
        Class<?> rawClass = Reflects.getRawClass(type);
        Number source;
        if (Strings.isEmpty(value)) {
            return Converter.defaultValue(rawClass);
        } else if (value instanceof Number) {
            source = (Number) value;
        } else if (value instanceof Boolean) {
            source = (Boolean) value ? 1 : 0;
        } else if (value instanceof byte[]) {
            source = Double.parseDouble(new String((byte[]) value));
        } else {
            source = Double.parseDouble(value.toString().trim());
        }
        if (Boolean.class.isAssignableFrom(rawClass) || Boolean.TYPE.isAssignableFrom(rawClass)) {
            return source.intValue() != 0;
        }
        if (Character.class.isAssignableFrom(rawClass) || Character.TYPE.isAssignableFrom(rawClass)) {
            return (char) source.byteValue();
        }
        if (Short.class.isAssignableFrom(rawClass) || Short.TYPE.isAssignableFrom(rawClass)) {
            return source.shortValue();
        }
        if (Integer.class.isAssignableFrom(rawClass) || Integer.TYPE.isAssignableFrom(rawClass)) {
            return source.intValue();
        }
        if (Long.class.isAssignableFrom(rawClass) || Long.TYPE.isAssignableFrom(rawClass)) {
            return source.longValue();
        }
        if (Float.class.isAssignableFrom(rawClass) || Float.TYPE.isAssignableFrom(rawClass)) {
            return source.floatValue();
        }
        if (Double.class.isAssignableFrom(rawClass) || Double.TYPE.isAssignableFrom(rawClass)) {
            return source.doubleValue();
        }
        return source.intValue();
    }

    public String toString() {
        return "number";
    }
}

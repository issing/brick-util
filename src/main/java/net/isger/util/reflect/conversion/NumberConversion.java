package net.isger.util.reflect.conversion;

import net.isger.util.reflect.Converter;

public class NumberConversion implements Conversion {

    public static final NumberConversion CONVERSION = new NumberConversion();

    private NumberConversion() {
    }

    public boolean isSupport(Class<?> type) {
        return Boolean.class.isAssignableFrom(type)
                || Character.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type) || type.isPrimitive();
    }

    public Object convert(Class<?> type, Object value) {
        Number source;
        if (value == null) {
            return (Number) Converter.defaultValue(type);
        } else if (value instanceof Number) {
            source = (Number) value;
        } else if (value instanceof Boolean) {
            source = (Boolean) value ? 1 : 0;
        } else if (value instanceof byte[]) {
            source = Double.parseDouble(new String((byte[]) value));
        } else {
            source = Double.parseDouble(value.toString().trim());
        }
        if (Boolean.class.isAssignableFrom(type)
                || Boolean.TYPE.isAssignableFrom(type)) {
            return source.intValue() != 0;
        }
        if (Character.class.isAssignableFrom(type)
                || Character.TYPE.isAssignableFrom(type)) {
            return (char) source.byteValue();
        }
        if (Integer.class.isAssignableFrom(type)
                || Integer.TYPE.isAssignableFrom(type)) {
            return source.intValue();
        }
        if (Long.class.isAssignableFrom(type)
                || Long.TYPE.isAssignableFrom(type)) {
            return source.longValue();
        }
        if (Float.class.isAssignableFrom(type)
                || Float.TYPE.isAssignableFrom(type)) {
            return source.floatValue();
        }
        if (Double.class.isAssignableFrom(type)
                || Double.TYPE.isAssignableFrom(type)) {
            return source.doubleValue();
        }
        return source.intValue();
    }

    public String toString() {
        return "number";
    }
}

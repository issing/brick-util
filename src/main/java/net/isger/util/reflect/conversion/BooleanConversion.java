package net.isger.util.reflect.conversion;

import net.isger.util.reflect.Conversion;

public class BooleanConversion implements Conversion {

    public static final BooleanConversion CONVERSION = new BooleanConversion();

    private BooleanConversion() {
    }

    public boolean isSupport(Class<?> type) {
        return Boolean.class.isAssignableFrom(type)
                || Boolean.TYPE.isAssignableFrom(type);
    }

    public Object convert(Class<?> type, Object value) {
        return value == null ? false : Boolean.parseBoolean(value.toString());
    }

    public String toString() {
        return Boolean.class.getName();
    }
}

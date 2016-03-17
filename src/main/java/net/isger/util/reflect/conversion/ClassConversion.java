package net.isger.util.reflect.conversion;

import net.isger.util.Reflects;
import net.isger.util.reflect.Conversion;

public class ClassConversion implements Conversion {

    public static final ClassConversion CONVERSION = new ClassConversion();

    private ClassConversion() {
    }

    public boolean isSupport(Class<?> type) {
        return type.equals(Class.class);
    }

    public Object convert(Class<?> type, Object value) {
        if (value instanceof String) {
            Object result = Reflects.getClass((String) value);
            if (result != null) {
                return result;
            }
        }
        throw new IllegalStateException("Unexpected class conversion for "
                + value);
    }

    public String toString() {
        return Class.class.getName();
    }

}

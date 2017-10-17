package net.isger.util.reflect.conversion;

import java.lang.reflect.Type;

import net.isger.util.Asserts;
import net.isger.util.Reflects;

public class ClassConversion implements Conversion {

    public static final ClassConversion CONVERSION = new ClassConversion();

    private ClassConversion() {
    }

    public boolean isSupport(Type type) {
        return Reflects.getRawClass(type).equals(Class.class);
    }

    public Object convert(Type type, Object value) {
        if (value instanceof String) {
            Object result = Reflects.getClass((String) value);
            if (result != null) {
                return result;
            }
        }
        throw Asserts.state("Unexpected class conversion for %s", value);
    }

    public String toString() {
        return "class";
    }

}

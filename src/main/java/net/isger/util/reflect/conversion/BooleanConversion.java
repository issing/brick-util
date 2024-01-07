package net.isger.util.reflect.conversion;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;

import net.isger.util.Helpers;
import net.isger.util.Reflects;
import net.isger.util.reflect.ClassAssembler;

public class BooleanConversion implements Conversion {

    public static final BooleanConversion CONVERSION = new BooleanConversion();

    private BooleanConversion() {
    }

    public boolean isSupport(Type type) {
        Class<?> rawClass = Reflects.getRawClass(type);
        return Boolean.class.isAssignableFrom(rawClass) || Boolean.TYPE.isAssignableFrom(rawClass);
    }

    public Object convert(Type type, Object value, ClassAssembler assembler) {
        return value instanceof JsonElement ? ((JsonElement) value).getAsBoolean() : Helpers.toBoolean(value);
    }

    public String toString() {
        return "boolean";
    }
}

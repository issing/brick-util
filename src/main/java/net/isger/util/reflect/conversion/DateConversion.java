package net.isger.util.reflect.conversion;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Date;

import com.google.gson.JsonElement;

import net.isger.util.Asserts;
import net.isger.util.Dates;
import net.isger.util.Reflects;
import net.isger.util.Strings;
import net.isger.util.reflect.ClassAssembler;

public class DateConversion implements Conversion {

    public static final DateConversion CONVERSION = new DateConversion();

    private DateConversion() {
    }

    public boolean isSupport(Type type) {
        Class<?> rawClass = Reflects.getRawClass(type);
        return Date.class.isAssignableFrom(rawClass);
    }

    public Date convert(Type type, Object value, ClassAssembler assembler) {
        String source;
        convert: {
            if (Strings.isEmpty(value)) {
                return null;
            } else if (value instanceof byte[]) {
                source = new String((byte[]) value);
            } else if (value instanceof Number) {
                source = value.toString();
            } else if (value instanceof JsonElement) {
                source = ((JsonElement) value).getAsString();
            } else if (!(value instanceof String)) {
                source = value.toString();
                break convert;
            } else {
                source = (String) value;
            }
            try {
                return new Date(Double.valueOf(source).longValue());
            } catch (Exception e) {
                value = source;
            }
        }
        try {
            Method method = Reflects.getRawClass(type).getDeclaredMethod("valueOf");
            return (Date) method.invoke(type, value);
        } catch (Exception e) {
        }
        Date date = Dates.toDate(value);
        if (date == null) throw Asserts.argument(source);
        return date;
    }

    public String toString() {
        return "date";
    }

}

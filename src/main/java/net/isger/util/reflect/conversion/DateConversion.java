package net.isger.util.reflect.conversion;

import java.lang.reflect.Method;
import java.util.Date;

import net.isger.util.Dates;
import net.isger.util.reflect.Conversion;

public class DateConversion implements Conversion {

    public static final DateConversion CONVERSION = new DateConversion();

    private DateConversion() {
    }

    public boolean isSupport(Class<?> type) {
        return type.equals(Date.class) || Date.class.isAssignableFrom(type);
    }

    public Date convert(Class<?> type, Object value) {
        String source;
        if (value == null || (source = value.toString()).length() == 0) {
            return null;
        }
        try {
            Method method = type.getDeclaredMethod("valueOf");
            return (Date) method.invoke(type, source);
        } catch (Exception e) {
        }
        Date date = Dates.toDate(value);
        if (date == null) {
            throw new IllegalArgumentException(source);
        }
        return date;
    }

    public String toString() {
        return Date.class.getName();
    }

}

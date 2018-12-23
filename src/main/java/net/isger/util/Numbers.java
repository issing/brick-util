package net.isger.util;

public class Numbers {

    private Numbers() {
    }

    public static long parseLong(Object value) {
        return parseLong(value, -1);
    }

    public static long parseLong(Object value, long def) {
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Double.valueOf(value.toString()).longValue();
        } catch (Throwable e) {
            return def;
        }
    }

    public static int parseInt(Object value) {
        return parseInt(value, -1);
    }

    public static int parseInt(Object value, int def) {
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Double.valueOf(value.toString()).intValue();
        } catch (Throwable e) {
            return def;
        }
    }

}

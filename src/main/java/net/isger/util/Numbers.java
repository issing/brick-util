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
            return Long.parseLong(value.toString());
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
            return Integer.parseInt(value.toString());
        } catch (Throwable e) {
            return def;
        }
    }

}

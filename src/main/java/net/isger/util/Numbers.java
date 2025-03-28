package net.isger.util;

public class Numbers {

    private Numbers() {
    }

    /**
     * 转换整形
     *
     * @param value
     * @return
     */
    public static int toInt(Object value) {
        return toInt(value, 0);
    }

    /**
     * 转换整形
     * 
     * @param value
     * @param def
     * @return
     */
    public static int toInt(Object value, int def) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value != null) {
            try {
                def = Double.valueOf(value.toString()).intValue();
            } catch (Exception e) {
            }
        }
        return def;
    }

    /**
     * 转换长整形
     *
     * @param value
     * @return
     */
    public static long toLong(Object value) {
        return toLong(value, 0);
    }

    /**
     * 转换长整形
     * 
     * @param value
     * @param def
     * @return
     */
    public static long toLong(Object value, long def) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value != null) {
            try {
                def = Double.valueOf(value.toString()).longValue();
            } catch (Exception e) {
            }
        }
        return def;
    }

    /**
     * 转换双精度
     *
     * @param value
     * @return
     */
    public static double toDouble(Object value) {
        return toDouble(value, 0);
    }

    /**
     * 转换双精度
     * 
     * @param value
     * @param def
     * @return
     */
    public static double toDouble(Object value, double def) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value != null) {
            try {
                def = Double.valueOf(value.toString());
            } catch (Exception e) {
            }
        }
        return def;
    }

}

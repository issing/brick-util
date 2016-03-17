package net.isger.util;

import java.util.HashMap;
import java.util.Map;

public class Numbers {

    private final static char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
            'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
            'X', 'Y', 'Z' };

    private final static Map<Character, Integer> digitMap = new HashMap<Character, Integer>();

    /** 最大进制数 */
    public static final int MAX_RADIX = DIGITS.length;

    /** 最小进制数 */
    public static final int MIN_RADIX = 2;

    static {
        for (int i = 0; i < DIGITS.length; i++) {
            digitMap.put(DIGITS[i], (int) i);
        }
    }

    private Numbers() {
    }

    /**
     * 将长整型数值转换为指定的进制数
     * 
     * @param value
     * @param radix
     * @return
     */
    public static String parse(long value, int radix) {
        if (radix < MIN_RADIX || radix > MAX_RADIX || radix == 10) {
            return Long.toString(value);
        }
        final int size = 65;
        int pos = 64;
        char[] buffer = new char[size];
        boolean negative = value < 0;
        if (!negative) {
            value = -value;
        }
        while (value <= -radix) {
            buffer[pos--] = DIGITS[(int) (-(value % radix))];
            value = value / radix;
        }
        buffer[pos] = DIGITS[(int) (-value)];
        if (negative) {
            buffer[--pos] = '-';
        }
        return new String(buffer, pos, size - pos);
    }

    /**
     * 将字符串转换为指定的进制数长整型数值
     * 
     * @param value
     * @param radix
     * @return
     */
    public static long parse(String value, int radix) {
        int count = (value = value.trim()).length();
        if (radix < MIN_RADIX || radix > MAX_RADIX) {
            throw new NumberFormatException("Unsupported " + radix + " radix");
        } else if (radix <= 36) {
            value = value.toLowerCase();
        }
        if (count == 0) {
            throw new NumberFormatException("Failure to parse \"" + value
                    + "\" using " + radix + " radix");
        }
        long limit = -Long.MAX_VALUE;
        Integer digit;
        boolean negative = false;
        int amount = 0;
        char v = value.charAt(0);
        if (v < '0') {
            if (v == '-') {
                negative = true;
                limit = Long.MIN_VALUE;
            } else if (v != '+' || count == 1) {
                throw new NumberFormatException("Failure to parse \"" + value
                        + "\" using " + radix + " radix");
            }
            amount++;
        }
        long result = 0;
        long multmin = limit / radix;
        while (amount < count) {
            digit = digitMap.get(value.charAt(amount++));
            if (digit == null || digit < 0 || result < multmin) {
                throw new NumberFormatException("Failure to parse \"" + value
                        + "\" using " + radix + " radix");
            }
            result *= radix;
            if (result < limit + digit) {
                throw new NumberFormatException("Failure to parse \"" + value
                        + "\" using " + radix + " radix");
            }
            result -= digit;
        }
        return negative ? result : -result;
    }

}

package net.isger.util;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import net.isger.util.anno.Alias;
import net.isger.util.anno.Ignore;
import net.isger.util.anno.Ignore.Mode;

/**
 * 帮助工具
 * 
 * @author issing
 *
 */
public class Helpers {

    private final static char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
            'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
            'X', 'Y', 'Z' };

    /** 最大进制数 */
    public static final int MAX_RADIX = DIGITS.length;

    /** 最小进制数 */
    public static final int MIN_RADIX = 2;

    private final static Map<Character, Integer> DIGIT_INDECES;

    static {
        DIGIT_INDECES = new HashMap<Character, Integer>();
        for (int i = 0; i < DIGITS.length; i++) {
            DIGIT_INDECES.put(DIGITS[i], (int) i);
        }
    }

    private Helpers() {
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
            digit = DIGIT_INDECES.get(value.charAt(amount++));
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

    /**
     * 生成19位UUID
     * 
     * @return
     */
    public static String makeUUID() {
        UUID uuid = UUID.randomUUID();
        StringBuilder buffer = new StringBuilder();
        buffer.append(digits(uuid.getMostSignificantBits() >> 32, 8));
        buffer.append(digits(uuid.getMostSignificantBits() >> 16, 4));
        buffer.append(digits(uuid.getMostSignificantBits(), 4));
        buffer.append(digits(uuid.getLeastSignificantBits() >> 48, 4));
        buffer.append(digits(uuid.getLeastSignificantBits(), 12));
        return buffer.toString();
    }

    private static String digits(long val, int digits) {
        long hi = 1L << (digits * 4);
        return parse(hi | (val & (hi - 1)), MAX_RADIX).substring(1);
    }

    public static URL getURL(File file) {
        try {
            return file.getAbsoluteFile().toURI().toURL();
        } catch (Exception e) {
        }
        return null;
    }

    public static URL getPropertiesURL(boolean isXML, String name) {
        return getPropertiesURL(isXML, null, name);
    }

    public static URL getPropertiesURL(boolean isXML, Object source, String name) {
        URL url = Reflects.getResource(source, name);
        if (url == null) {
            if (isXML) {
                if (!Strings.endWithIgnoreCase(name, "[.]xml")) {
                    url = Reflects.getResource(source, name + ".xml");
                }
            } else {
                if (!Strings.endWithIgnoreCase(name, "[.]properties")) {
                    url = Reflects.getResource(source, name + ".properties");
                }
            }
        }
        return url;
    }

    public static Properties getProperties(File file) {
        return load(new Properties(), file);
    }

    public static Properties getProperties(String name) {
        return load(getProperties(false, name), true, name);
    }

    public static Properties getProperties(Object source, String name) {
        return load(getProperties(false, source, name), true, source, name);
    }

    public static Properties getProperties(boolean isXML, String name) {
        return getProperties(isXML, getPropertiesURL(isXML, name));
    }

    public static Properties getProperties(boolean isXML, Object source,
            String name) {
        return getProperties(isXML, getPropertiesURL(isXML, source, name));
    }

    public static Properties getProperties(boolean isXML, URL url) {
        return load(new Properties(), isXML, url);
    }

    public static Properties load(Properties props, File file) {
        URL url = getURL(file);
        return load(load(props, false, url), true, url);
    }

    public static Properties load(Properties props, String name) {
        return load(load(props, false, name), true, name);
    }

    public static Properties load(Properties props, Object source, String name) {
        return load(load(props, false, source, name), true, source, name);
    }

    public static Properties load(Properties props, boolean isXML, String name) {
        return load(props, isXML, getPropertiesURL(isXML, name));
    }

    public static Properties load(Properties props, boolean isXML,
            Object source, String name) {
        return load(props, isXML, getPropertiesURL(isXML, source, name));
    }

    public static Properties load(Properties props, boolean isXML, URL url) {
        InputStream in = null;
        try {
            in = url.openStream();
            if (isXML) {
                props.loadFromXML(in);
            } else {
                props.load(in);
            }
        } catch (Exception e) {
        } finally {
            Files.close(in);
        }
        return props;
    }

    public static String getProperty(String key) {
        return getProperty(key, "");
    }

    public static String getProperty(final String key, final String value) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty(key, value);
            }
        });
    }

    public static boolean hasAliasName(Class<?> clazz) {
        Alias alias = clazz.getAnnotation(Alias.class);
        boolean result = alias != null;
        if (result) {
            result = Strings.isNotEmpty(alias.value());
        }
        return result;
    }

    public static boolean hasAliasName(Annotation[] annos) {
        boolean result = annos != null && annos.length > 0;
        if (result) {
            for (Annotation anno : annos) {
                if (anno instanceof Alias) {
                    result = Strings.isNotEmpty(((Alias) anno).value());
                    break;
                }
            }
        }
        return result;
    }

    public static String getAliasName(Annotation[] annos) {
        if (annos != null && annos.length > 0) {
            for (Annotation anno : annos) {
                if (anno instanceof Alias) {
                    return ((Alias) anno).value();
                }
            }
        }
        return null;
    }

    public static String getAliasName(Class<?> clazz) {
        return getAliasName(clazz, null, null);
    }

    public static String getAliasName(Class<?> clazz, String mask) {
        return getAliasName(clazz, mask, null);
    }

    public static String getAliasName(Class<?> clazz, String mask, String value) {
        String name;
        if (hasAliasName(clazz)) {
            name = clazz.getAnnotation(Alias.class).value().trim();
        } else if (Strings.isNotEmpty(value)) {
            name = value.trim();
        } else {
            name = Strings.toLower(clazz.getSimpleName());
        }
        return Strings.isEmpty(mask) ? name : Strings.replaceIgnoreCase(name,
                mask);
    }

    /**
     * 集合规范化
     * 
     * @param values
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> canonicalize(Map<String, Object> values) {
        Map<String, Object> result = new HashMap<String, Object>();
        int index;
        String key;
        String subKey;
        Object value;
        Map<String, Object> container;
        for (Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            key = entry.getKey();
            index = key.indexOf(".");
            if (index == -1) {
                subKey = null;
            } else {
                subKey = key.substring(index + 1);
                key = key.substring(0, index);
            }
            value = result.get(key);
            if (value == null) {
                if (subKey == null) {
                    result.put(key, entry.getValue());
                    continue;
                }
                container = new HashMap<String, Object>();
                result.put(key, container);
            } else if (value instanceof Map) {
                container = (Map<String, Object>) value;
                if (subKey == null) {
                    throw new IllegalStateException();
                }
            } else {
                throw new IllegalStateException();
            }
            container.put(subKey, entry.getValue());
        }
        return result;
    }

    public static boolean contains(List<?> a, List<?> b) {
        boolean result = a.size() >= b.size();
        for (Object v : b) {
            if (!a.contains(v)) {
                result = false;
                break;
            }
        }
        return result;
    }

    public static <T> List<T> getSurplus(List<T> a, List<T> b) {
        List<T> result = new ArrayList<T>();
        for (T v : b) {
            if (!a.contains(v)) {
                result.add(v);
            }
        }
        return result;
    }

    public static <T> List<T> getMerge(List<T> a, List<T> b) {
        if (a == null || a.size() == 0)
            return b;
        if (b == null || b.size() == 0)
            return a;
        List<T> container = getMerge(new ArrayList<T>(), a);
        for (T t : b) {
            if (!(t == null || container.contains(t))) {
                container.add(t);
            }
        }
        return container;
    }

    public static <K, V> Map<K, V> copyMap(Map<K, V> source) {
        Map<K, V> target = new HashMap<K, V>();
        if (source != null) {
            target.putAll(source);
        }
        return target;
    }

    public static Object copyArray(Object source, int length) {
        Object target;
        Class<?> clazz = source == null ? Object.class : source.getClass();
        if (clazz.isArray()) {
            target = Array.newInstance(clazz.getComponentType(), length);
            length = Math.min(Array.getLength(source), length);
            System.arraycopy(source, 0, target, 0, length);
        } else {
            target = Array.newInstance(clazz, length);
            Array.set(target, 0, source);
        }
        return target;
    }

    public static Object wrap(Object... args) {
        return args;
    }

    public static Object[] wraps(Object... args) {
        return args;
    }

    @SafeVarargs
    public static <T> T[] group(T... args) {
        return args;
    }

    public static Mode getMode(Ignore ignore, Mode mode) {
        if (ignore != null) {
            Mode result = ignore.mode();
            if (result != null) {
                return result;
            }
        }
        return mode;
    }

}

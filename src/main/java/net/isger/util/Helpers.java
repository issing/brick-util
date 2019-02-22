package net.isger.util;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import net.isger.util.anno.Alias;

/**
 * 帮助工具
 * 
 * @author issing
 *
 */
public class Helpers {

    public static final Logger LOG;

    public static final int SEED_DIGIT = 0;

    private static final Object UUID_LOCKED = new Object();

    private static int uuidSearial = 0;

    private static final String REGEX_CODE = "[A-Z0-9]+(\\-[A-Z0-9]+)*";

    private static final String TABLE_RADIX = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final char[] CODES = TABLE_RADIX.toCharArray();

    private static final int[][] CODES_LIMITS = { { 0, 10 }, { 10, 26 },
            { 36, 26 }, { 0, 16 }, { 0, 36 }, { 10, 52 }, { 0, 62 } };

    /** 最大进制数 */
    public static final int MAX_RADIX = CODES.length;

    /** 最小进制数 */
    public static final int MIN_RADIX = 2;

    private final static Map<Character, Integer> DIGIT_INDECES;

    private static final Gson GSON;

    static {
        LOG = LoggerFactory.getLogger(Helpers.class);
        DIGIT_INDECES = new HashMap<Character, Integer>();
        for (int i = 0; i < MAX_RADIX; i++) {
            DIGIT_INDECES.put(CODES[i], (int) i);
        }
        GSON = new Gson();
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
            buffer[pos--] = CODES[(int) (-(value % radix))];
            value = value / radix;
        }
        buffer[pos] = CODES[(int) (-value)];
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

    public static String toJson(Object instance) {
        return GSON.toJson(instance);
    }

    public static Object fromJson(String json) {
        return fromJson(json, Object.class);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return GSON.fromJson(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 转换布尔值
     * 
     * @param value
     * @return
     */
    public static boolean toBoolean(Object value) {
        return value != null && (value instanceof Boolean ? (boolean) value
                : Boolean.parseBoolean(value.toString())
                        || (toInt(value, 0) != 0)
                        || "t".equalsIgnoreCase(value.toString())
                        || "y".equalsIgnoreCase(value.toString())
                        || "yes".equalsIgnoreCase(value.toString()));
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

    /**
     * 转换16进制
     * 
     * @param values
     * @return
     */
    public static String toHex(byte[] values) {
        int hex;
        StringBuffer buffer = new StringBuffer(values.length);
        for (byte value : values) {
            if ((hex = value & 0xFF) < 16) {
                buffer.append("0");
            }
            buffer.append(Integer.toHexString(hex));
        }
        return buffer.toString();
    }

    public static byte[] toHex(String hex) {
        int size = hex.length() / 2;
        byte[] result = new byte[size];
        char[] values = hex.toCharArray();
        int offset;
        byte radixHigh, radixLow;
        for (int i = 0; i < size; i++) {
            radixHigh = toRadix(values[offset = i * 2]);
            radixLow = toRadix(values[offset + 1]);
            if (radixHigh == -1 || radixLow == -1) {
                return null;
            }
            result[i] = (byte) (radixHigh << 4 | radixLow);
        }
        return result;
    }

    public static byte toRadix(char value) {
        return (byte) TABLE_RADIX.indexOf(value);
    }

    /**
     * 生成MD5摘要
     * 
     * @param value
     * @return
     */
    public static String makeMD5(byte[] value) {
        try {
            return toHex(Securities.toDigest("MD5", value));
        } catch (Exception e) {
            throw Asserts.state("Failure to make MD5 for [%s] - %s", value,
                    e.getMessage());
        }
    }

    /**
     * 生成MD5摘要
     * 
     * @param hex
     * @return
     */
    public static String makeMD5(String hex) {
        return makeMD5(toHex(hex));
    }

    /**
     * 生成20位UUID
     * 
     * @return
     */
    public static String makeUUID() {
        UUID uuid = UUID.randomUUID();
        StringBuilder buffer = new StringBuilder();
        long mostBits = uuid.getMostSignificantBits();
        buffer.append(getDigits(mostBits >> 32, 8));
        buffer.append(getDigits(mostBits >> 16, 4));
        buffer.append(getDigits(mostBits, 4));
        long leastBits = uuid.getLeastSignificantBits();
        buffer.append(getDigits(leastBits >> 48, 4));
        buffer.append(getDigits(leastBits, 12));
        synchronized (UUID_LOCKED) {
            buffer.append(CODES[uuidSearial]);
            if (++uuidSearial >= MAX_RADIX) {
                uuidSearial = 0;
            }
        }
        return buffer.toString();
    }

    /**
     * 获取指定位数值
     *
     * @param value
     * @param bits
     * @return
     */
    private static String getDigits(long value, int bits) {
        long hi = 1L << (bits * 4);
        return parse(hi | (value & (hi - 1)), MAX_RADIX).substring(1);
    }

    /**
     * 生成指定位数编码
     * 
     * @param length
     * @return
     */
    public static String makeCode(int length) {
        return makeCode(length, getRandom());
    }

    /**
     * 生成指定位数编码
     * 
     * @param length
     * @param seed
     * @return
     */
    public static String makeCode(int length, int seed) {
        StringBuffer code = new StringBuffer(length);
        for (int i = 0; i < length; i++) {
            code.append(getRandomCode(seed));
        }
        return code.toString();
    }

    /**
     * 生成随机码
     * 
     * @param seed
     * @return
     */
    private static char getRandomCode(int seed) {
        seed %= CODES_LIMITS.length;
        return getRandomCode(CODES_LIMITS[seed][0], CODES_LIMITS[seed][1]);
    }

    private static char getRandomCode(int start, int limit) {
        return CODES[start + getRandom(limit)];
    }

    /**
     * 获取随机数
     * 
     * @return
     */
    public static int getRandom() {
        return getRandom(0);
    }

    /**
     * 获取随机数
     * 
     * @param limit
     * @return
     */
    public static int getRandom(int limit) {
        UUID uuid = UUID.randomUUID();
        Random random = new Random(uuid.getMostSignificantBits());
        int seed = Math.abs(
                (int) (uuid.getLeastSignificantBits() % Integer.MAX_VALUE));
        int result;
        while ((result = random.nextInt(seed)) < 0) {
            continue;
        }
        return limit <= 0 ? result : result % limit;
    }

    public static int getBitCount(long value) {
        return Long.bitCount(value);
    }

    public static int getBitCount(byte[] bytes) {
        int count = 0;
        for (byte value : bytes) {
            count += Integer.bitCount(value & 0xff);
        }
        return count;
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

    public static URL getPropertiesURL(boolean isXML, Object source,
            String name) {
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

    public static Properties load(Properties props, Object source,
            String name) {
        return load(load(props, false, source, name), true, source, name);
    }

    public static Properties load(Properties props, boolean isXML,
            String name) {
        return load(props, isXML, getPropertiesURL(isXML, name));
    }

    public static Properties load(Properties props, boolean isXML,
            Object source, String name) {
        return load(props, isXML, getPropertiesURL(isXML, source, name));
    }

    public static Properties load(Properties props, boolean isXML, URL url) {
        if (url == null) {
            return props;
        }
        InputStream in = null;
        try {
            in = url.openStream();
            if (isXML) {
                props.loadFromXML(in);
            } else {
                props.load(in);
            }
        } catch (Exception e) {
            LOG.warn("Failed to load properties [{}]", url, e);
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
        return Strings
                .isNotEmpty(getAliasName(clazz.getAnnotation(Alias.class)))
                || Strings.isNotEmpty(getAliasName(
                        clazz.getAnnotation(javax.inject.Named.class)));
    }

    public static boolean hasAliasName(Annotation[] annos) {
        boolean result = false;
        if (annos != null) {
            for (Annotation anno : annos) {
                if (Strings.isNotEmpty(getAliasName(anno))) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public static String getAliasName(Annotation[] annos) {
        String name = null;
        if (annos != null) {
            for (Annotation anno : annos) {
                if (Strings.isNotEmpty(name = getAliasName(anno))) {
                    break;
                }
            }
        }
        return name;
    }

    public static String getAliasName(Class<?> clazz) {
        return getAliasName(clazz, null, null);
    }

    public static String getAliasName(Class<?> clazz, String mask) {
        return getAliasName(clazz, mask, null);
    }

    public static String getAliasName(Class<?> clazz, String mask,
            String value) {
        String name;
        if (hasAliasName(clazz)) {
            name = Strings.empty(getAliasName(clazz.getAnnotation(Alias.class)),
                    getAliasName(
                            clazz.getAnnotation(javax.inject.Named.class)));
        } else if (Strings.isNotEmpty(value)) {
            name = value.trim();
        } else {
            name = clazz.getSimpleName();
            if (Helpers.toColumnName(name).startsWith("i_")) {
                name = name.substring(1);
            }
            name = name.toLowerCase();
        }
        return Strings.isEmpty(mask) ? name
                : Strings.replaceIgnoreCase(name, mask);
    }

    private static String getAliasName(Annotation anno) {
        String name = null;
        if (anno instanceof Alias) {
            name = ((Alias) anno).value();
        } else if (anno instanceof javax.inject.Named) {
            name = ((javax.inject.Named) anno).value();
        }
        return name;
    }

    /**
     * 转换集合
     *
     * @param content
     * @return
     */
    public static Map<String, Object> toMap(String content) {
        Map<String, Object> result = new HashMap<String, Object>();
        String[] pending = content.split("[\\&]");
        String[] entry;
        for (String pair : pending) {
            entry = pair.split("[\\=]", 2);
            if (entry.length == 2) {
                result.put(entry[0], entry[1]);
            }
        }
        return result;
    }

    /**
     * 屏蔽集合修改功能
     * 
     * @param values
     * @return
     */
    public static <T> Map<String, List<T>> toUnmodifiable(
            Map<String, List<T>> values) {
        for (Entry<String, List<T>> entry : values.entrySet()) {
            values.put(entry.getKey(),
                    Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(values);
    }

    /**
     * 集合层级化
     * 
     * @param values
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toHierarchical(
            Map<String, Object> values) {
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

    /**
     * 集合扁平化
     *
     * @param values
     * @return
     */
    public static Map<String, Object> toFlat(Map<String, Object> values) {
        return null;
    }

    /**
     * 追加实例
     * 
     * @param values
     * @param name
     * @param value
     * @return
     */
    public static <K, T> boolean toAppend(Map<K, List<T>> values, K name,
            T value) {
        return toAppend(values, name, value, true);
    }

    /**
     * 追加实例
     *
     * @param values
     * @param name
     * @param value
     * @param repeat
     * @return
     */
    public static <K, T> boolean toAppend(Map<K, List<T>> values, K name,
            T value, boolean repeat) {
        return toAppends(values, name, Arrays.asList(value), repeat) > 0;
    }

    /**
     * 追加实例
     * 
     * @param values
     * @param name
     * @param value
     * @return
     */
    public static <K, T> int toAppends(Map<K, List<T>> values, K name,
            Collection<T> value) {
        return toAppends(values, name, value, true);
    }

    /**
     * 追加实例
     * 
     * @param values
     * @param name
     * @param value
     * @return
     */
    public static <K, T> int toAppends(Map<K, List<T>> values, K name,
            Collection<T> value, boolean repeat) {
        List<T> container = values.get(name);
        if (container == null) {
            values.put(name, container = new ArrayList<T>());
        }
        return repeat ? (container.addAll(value) ? value.size() : 0)
                : add(container, value);
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

    /**
     * 获取目标盈余
     * 
     * @param source
     * @param target
     * @return
     */
    public static <T> List<T> getSurplus(List<T> source, List<T> target) {
        List<T> result = new ArrayList<T>();
        for (T v : target) {
            if (!source.contains(v)) {
                result.add(v);
            }
        }
        return result;
    }

    /**
     * 获取合并结果
     * 
     * @param source
     * @param target
     * @return
     */
    public static <T> List<T> getMerge(List<T> source, List<T> target) {
        List<T> container = new ArrayList<T>();
        if (source != null && source.size() > 0) {
            add(container, target);
        }
        if (target != null && target.size() > 0) {
            add(container, target);
        }
        return container;
    }

    public static <T> int add(Collection<T> container, Enumeration<T> values) {
        return add(container, getList(values));
    }

    public static <T> int add(Collection<T> container, Iterator<T> values) {
        return add(container, getList(values));
    }

    @SuppressWarnings("unchecked")
    public static <T> int add(Collection<T> container, T... values) {
        return add(container, values == null ? null : Arrays.asList(values));
    }

    public static <T> int add(Collection<T> container, Collection<T> values) {
        int amount = 0;
        if (values != null) {
            for (T value : values) {
                if (add(container, value)) {
                    amount++;
                }
            }
        }
        return amount;
    }

    public static <T> boolean add(Collection<T> container, T value) {
        boolean result;
        if (result = !(value == null || container.contains(value))) {
            container.add(value);
        }
        return result;
    }

    public static <T> List<T> getList(Enumeration<T> values) {
        List<T> result = new ArrayList<T>();
        while (values.hasMoreElements()) {
            result.add(values.nextElement());
        }
        return result;
    }

    public static <T> List<T> getList(Iterator<T> values) {
        List<T> result = new ArrayList<T>();
        while (values.hasNext()) {
            result.add(values.next());
        }
        return result;
    }

    public static <K, V> Map<K, V> getMap(Map<K, V> source) {
        Map<K, V> target = new HashMap<K, V>();
        if (source != null) {
            target.putAll(source);
        }
        return target;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> params,
            String namespace) {
        if (Strings.isNotEmpty(namespace)) {
            params = Helpers.toHierarchical(params);
            Object value;
            for (String name : namespace.split("[./]")) {
                value = params.get(name);
                if (value instanceof Map) {
                    params = (Map<String, Object>) value;
                } else {
                    params = null;
                    break;
                }
            }
        }
        return params;
    }

    public static Object getInstance(Map<String, Object> params,
            String namespace) {
        int index = namespace.lastIndexOf(".");
        if (index > 0) {
            params = getMap(params, namespace.substring(0, index));
            namespace = namespace.substring(index + 1);
        }
        return params.get(namespace);
    }

    public static Object toArray(Object source) {
        if (source == null) {
            return null;
        } else if (source instanceof Collection) {
            Collection<?> collection = (Collection<?>) source;
            return collection.toArray(new Object[collection.size()]);
        }
        Class<?> sourceClass = source.getClass();
        if (sourceClass.isArray()) {
            return source;
        } else {
            return new Object[] { source };
        }
    }

    public static Object newArray(Object source) {
        if (source == null) {
            return null;
        }
        Class<?> sourceClass = source.getClass();
        int sourceCount;
        if (sourceClass.isArray()) {
            sourceCount = Array.getLength(source);
            sourceClass = sourceClass.getComponentType();
        } else {
            sourceCount = 1;
            source = new Object[] { source };
        }
        Object result = Array.newInstance(sourceClass, sourceCount);
        System.arraycopy(source, 0, result, 0, sourceCount);
        return result;
    }

    public static Object newArray(Object source, int length) {
        Object result;
        Class<?> clazz = source == null ? Object.class : source.getClass();
        if (clazz.isArray()) {
            result = Array.newInstance(clazz.getComponentType(), length);
            length = Math.min(Array.getLength(source), length);
            System.arraycopy(source, 0, result, 0, length);
        } else {
            result = Array.newInstance(clazz, length);
            Array.set(result, 0, source);
        }
        return result;
    }

    public static Object newArray(Object source, Object target) {
        /* 数组合并检测 */
        target = newArray(target);
        if (source == null) {
            return target;
        }
        source = newArray(source);
        if (target == null) {
            return source;
        }
        int sourceCount = Array.getLength(source);
        if (sourceCount == 0) {
            return target;
        }
        int targetCount = Array.getLength(target);
        if (targetCount == 0) {
            return source;
        }
        /* 数组合并操作 */
        Class<?> sourceType = source.getClass().getComponentType();
        Class<?> targetType = target.getClass().getComponentType();
        Class<?> resultType;
        if (sourceType.isAssignableFrom(targetType)) {
            resultType = sourceType;
        } else if (targetType.isAssignableFrom(sourceType)) {
            resultType = targetType;
        } else {
            resultType = Object.class;
        }
        if (!(sourceType.isArray() || targetType.isArray())) {
            Object result = Array.newInstance(resultType,
                    sourceCount + targetCount);
            for (Object[] current : new Object[][] {
                    { sourceType, source, 0, sourceCount },
                    { targetType, target, sourceCount, targetCount } }) {
                if (resultType.isAssignableFrom((Class<?>) current[0])) {
                    System.arraycopy(current[1], 0, result,
                            (Integer) current[2], (Integer) current[3]);
                } else {
                    for (int i = 0; i < (Integer) current[3]; i++) {
                        Array.set(result, i + (Integer) current[2],
                                Array.get(current[1], i));
                    }
                }
            }
            return result;
        }
        return newArray(resultType, source, sourceCount, target, targetCount);
    }

    public static Object newArray(Class<?> resultType, Object source,
            int sourceCount, Object target, int targetCount) {
        Object overValue;
        int loopCount;
        int resultCount;
        if ((sourceCount - targetCount) >= 0) {
            overValue = source;
            loopCount = targetCount;
            resultCount = sourceCount;
        } else {
            overValue = target;
            loopCount = sourceCount;
            resultCount = targetCount;
        }
        Object result = Array.newInstance(resultType, resultCount);
        int amount = 0;
        do {
            Array.set(result, amount, newArray(Array.get(source, amount),
                    Array.get(target, amount)));
        } while (++amount < loopCount);
        while (amount < resultCount) {
            Array.set(result, amount, Array.get(overValue, amount));
            amount++;
        }
        return result;
    }

    public static Object newArray(Class<?> resultType, Object source,
            int sourceCount) {
        Object result = Array.newInstance(resultType, sourceCount);
        int amount = 0;
        do {
            Array.set(result, amount, Array.get(source, amount));
        } while (++amount < sourceCount);
        return result;
    }

    public static Object[][] newGrid(boolean isColumn, Object... values) {
        int size = values.length;
        final List<List<Object>> grid = new ArrayList<List<Object>>();
        if (isColumn) {
            Object result;
            for (int i = 0; i < size; i++) {
                final int colCount = i;
                result = each(values[i], new Callable<Object>() {
                    public Object call(Object... args) {
                        Integer rowIndex = (Integer) args[0];
                        List<Object> row;
                        int rowCount = grid.size();
                        if (rowIndex == rowCount) {
                            grid.add(row = new ArrayList<Object>());
                            // 列填充
                            for (int i = 0; i < colCount; i++) {
                                row.add(null);
                            }
                        } else {
                            row = grid.get(rowIndex);
                        }
                        return row.add(args[1]);
                    }
                });
                // 行填充
                for (int j = (result instanceof Object[])
                        ? ((Object[]) result).length
                        : 1; j < grid.size(); j++) {
                    grid.get(j).add(null);
                }
            }
            size = grid.size();
        } else {
            Object result;
            int colCount = 0;
            for (int i = 0; i < size; i++) {
                final List<Object> row = new ArrayList<Object>();
                grid.add(row);
                result = each(values[i], new Callable<Object>() {

                    public Object call(Object... args) {
                        return row.add(args[1]);
                    }
                });

                // 行填充
                int count = (result instanceof Object[])
                        ? ((Object[]) result).length
                        : 1;
                if (count > colCount) {
                    for (int j = 0; j < i; j++) {
                        for (int k = colCount; k < count; k++) {
                            grid.get(j).add(null);
                        }
                    }
                    colCount = count;
                }
                // 列填充
                else {
                    for (int j = count; j < colCount; j++) {
                        row.add(null);
                    }
                }
            }
        }
        Object[][] result = new Object[size][];
        for (int i = 0; i < size; i++) {
            result[i] = grid.get(i).toArray();
        }
        return result;
    }

    /**
     * 数组紧凑化
     *
     * @param value
     * @return
     */
    public static Object compact(Object value) {
        if (value != null) {
            if (value instanceof Collection) {
                value = ((Collection<?>) value).toArray();
            }
            Class<?> type = value.getClass();
            if (type.isArray()) {
                int size = Array.getLength(value);
                switch (size) {
                case 0:
                    value = null;
                    break;
                case 1:
                    value = compact(Array.get(value, 0));
                    break;
                default:
                    List<Object> container = new ArrayList<Object>();
                    Object pending;
                    for (int i = 0; i < size; i++) {
                        if ((pending = compact(Array.get(value, i))) != null) {
                            container.add(pending);
                        }
                    }
                    value = container.size() < 2 ? compact(container)
                            : container.toArray();
                }
            }
        }
        return value;
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

    @SuppressWarnings("unchecked")
    public static <T> T[][] groups(T[]... args) {
        return args;
    }

    public static void sleep(int s) {
        sleep(TimeUnit.SECONDS.toMillis(s), 0);
    }

    public static void sleep(long ms) {
        sleep(ms, 0);
    }

    public static void sleep(long ms, int ns) {
        if (ms > 0 && ns > 0) {
            try {
                Thread.sleep(ms, ns);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * 获取地址
     *
     * @return
     * @throws Exception
     */
    public static InetAddress getAddress() {
        try {
            InetAddress candidateAddress = null;
            // 遍历网络接口
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface
                    .getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = ifaces.nextElement();
                // 遍历接口IP地址
                for (Enumeration<InetAddress> inetAddrs = iface
                        .getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs
                            .nextElement();
                    // 排除loopback类型地址
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            // 返回site-local地址
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // 记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 默认本机地址方案
            return InetAddress.getLocalHost();
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * 获取地址
     * 
     * @param port
     * @return
     */
    public static SocketAddress getAddress(int port) {
        return getAddress(null, port);
    }

    /**
     * 获取地址
     * 
     * @param host
     * @param port
     * @return
     */
    public static SocketAddress getAddress(String host, int port) {
        if (Strings.isNotEmpty(host)) {
            try {
                return new InetSocketAddress(InetAddress.getByName(host), port);
            } catch (UnknownHostException e) {
                return null;
            }
        }
        return new InetSocketAddress(port);
    }

    public static boolean isCode(String value) {
        return isCode(value, 64);
    }

    public static boolean isCode(String value, int limit) {
        if (limit <= 0) {
            limit = 64;
        }
        return Strings.isNotEmpty(value)
                && value.toUpperCase().matches(REGEX_CODE)
                && value.length() < limit;
    }

    public static boolean isMultiple(Object instance) {
        return instance instanceof Collection || instance.getClass().isArray();
    }

    /**
     * 遍历操作
     * 
     * @param instance
     * @param callable
     * @param args
     * @return
     */
    public static <T extends Object> Object each(Object instance,
            Callable<T> callable, Object... args) {
        return each(false, instance, callable, args);
    }

    /**
     * 遍历操作
     * 
     *
     * @param multipled
     * @param instance
     * @param callable
     * @param args
     * @return
     */
    public static <T extends Object> Object each(boolean multipled,
            Object instance, Callable<T> callable, Object... args) {
        boolean isMultiple = true;
        int size;
        if (instance instanceof Collection) {
            Collection<?> collection = (Collection<?>) instance;
            size = collection.size();
            instance = collection.toArray(new Object[size]);
        } else if (instance.getClass().isArray()) {
            size = Array.getLength(instance);
        } else {
            isMultiple = false;
            size = 1;
            instance = new Object[] { instance };
        }
        Object[] result = new Object[size];
        for (int i = 0; i < size; i++) {
            result[i] = callable.call(i, Array.get(instance, i), args);
        }
        return isMultiple || multipled ? result : result[0];
    }

    public static int getIndex(Object array, Object instance) {
        int size = 0;
        if (array instanceof Collection) {
            Collection<?> collection = (Collection<?>) array;
            size = collection.size();
            array = collection.toArray(new Object[size]);
        } else if (array.getClass().isArray()) {
            size = Array.getLength(array);
        }
        for (int i = 0; i < size; i++) {
            if (instance.equals(Array.get(array, i))) {
                return i;
            }
        }
        return -1;
    }

    public static Object getInstance(Object array, int index) {
        int size = 0;
        if (array instanceof Collection) {
            Collection<?> collection = (Collection<?>) array;
            size = collection.size();
            array = collection.toArray(new Object[size]);
        } else if (array.getClass().isArray()) {
            size = Array.getLength(array);
        }
        return index < 0 || index >= size ? null : Array.get(array, index);
    }

    public static Object nvl(Object value, Object alternative) {
        return nvl(value, value, alternative);
    }

    public static Object nvl(Object value, Object leftist, Object rightist) {
        return value == null ? rightist : leftist;
    }

    @SuppressWarnings("unchecked")
    public static <T> T coalesce(T... instances) {
        for (T instance : instances) {
            if (instance != null) {
                return instance;
            }
        }
        return null;
    }

    public static int toInt(String value, int beginIndex, int endIndex) {
        return toInt(10, value, beginIndex, endIndex);
    }

    public static int toInt(int radix, String value, int beginIndex,
            int endIndex) {
        return Integer.parseInt(value.substring(beginIndex, endIndex), radix);
    }

    /**
     * 转换为字段命名
     * 
     * @param columnName
     * @return
     */
    public static String toFieldName(String columnName) {
        char[] chs = columnName.toLowerCase().toCharArray();
        StringBuffer fieldName = new StringBuffer(chs.length);
        boolean hasUpper = false;
        for (char ch : chs) {
            // 跳过“_”符号，并设置接下来其它字符为大写
            if (ch == '_') {
                hasUpper = true;
                continue;
            } else if (hasUpper) {
                ch = Character.toUpperCase(ch);
                hasUpper = false; // 重置大写状态
            }
            fieldName.append(ch);
        }
        return fieldName.toString();
    }

    /**
     * 转换为列命名
     * 
     * @param fieldName
     * @return
     */
    public static String toColumnName(String fieldName) {
        // 去除所有“_”符号
        char[] chs = fieldName.replaceAll("[_]", "").toCharArray();
        StringBuffer columnName = new StringBuffer(chs.length + 16);
        for (char ch : chs) {
            // 遇大写字母前加“_”符号
            if (Character.isUpperCase(ch)) {
                columnName.append('_');
            }
            columnName.append(Character.toLowerCase(ch));
        }
        return columnName.toString();
    }

    public static int hashCode(Object instance) {
        return instance == null ? 0 : instance.hashCode();
    }

    public static boolean equals(Object source, Object target) {
        return source == target || (source != null && source.equals(target));
    }

    public static String getStackTrace(Throwable cause) {
        if (cause == null) {
            return null;
        }
        StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer, true));
        return writer.getBuffer().toString();
    }

}

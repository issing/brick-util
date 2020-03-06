package net.isger.util;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import net.isger.util.anno.Order;
import net.isger.util.reflect.BoundField;
import net.isger.util.reflect.Converter;

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

    private static final String CODE_RADIX = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final char[] CODES = CODE_RADIX.toCharArray();

    private static final int[][] CODES_LIMITS = { { 0, 10 }, { 10, 26 }, { 36, 26 }, { 0, 16 }, { 0, 36 }, { 10, 52 }, { 0, 62 } };

    /** 最大进制数 */
    public static final int MAX_RADIX = CODES.length;

    /** 最小进制数 */
    public static final int MIN_RADIX = 2;

    private final static Map<Character, Integer> DIGIT_INDECES;

    private static final Gson GSON;

    /** 属性配置缓存 */
    private final static Map<String, Properties> CACHE_PROPERTIES;

    static {
        LOG = LoggerFactory.getLogger(Helpers.class);
        CACHE_PROPERTIES = new HashMap<String, Properties>();
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
            throw new NumberFormatException("Failure to parse \"" + value + "\" using " + radix + " radix");
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
                throw new NumberFormatException("Failure to parse \"" + value + "\" using " + radix + " radix");
            }
            amount++;
        }
        long result = 0;
        long multmin = limit / radix;
        while (amount < count) {
            digit = DIGIT_INDECES.get(value.charAt(amount++));
            if (digit == null || digit < 0 || result < multmin) {
                throw new NumberFormatException("Failure to parse \"" + value + "\" using " + radix + " radix");
            }
            result *= radix;
            if (result < limit + digit) {
                throw new NumberFormatException("Failure to parse \"" + value + "\" using " + radix + " radix");
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

    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            if (Map.class.isAssignableFrom(clazz) || Collection.class.isAssignableFrom(clazz) || clazz == Object.class || clazz.isArray()) {
                return GSON.fromJson(json, clazz);
            }
            return Reflects.newInstance(clazz, (Map<String, Object>) fromJson(json, Map.class));
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
        return value != null && (value instanceof Boolean ? (boolean) value : toInt(value) != 0 || Strings.equalsIgnoreCase(value.toString(), "t", "true", "y", "yes"));
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
        return (byte) CODE_RADIX.indexOf(value);
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
            throw Asserts.state("Failure to make MD5 for [%s] - %s", value, e.getMessage());
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
            buffer.append(CODES[uuidSearial++]);
            uuidSearial %= MAX_RADIX;
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
        int seed = Math.abs((int) (uuid.getLeastSignificantBits() % Integer.MAX_VALUE));
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

    public static Properties getProperties(boolean isXML, Object source, String name) {
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

    public static Properties load(Properties props, boolean isXML, Object source, String name) {
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
            LOG.warn("(!) Failed to load properties [{}] {}- {}", url, isXML ? "from XML " : "", e.getMessage());
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

    public static String getProperty(Class<?> clazz, String id, Object... args) {
        return getProperty(clazz, null, id, args);
    }

    public static String getProperty(Class<?> clazz, String dialect, String id, Object... args) {
        return getProperty(null, clazz, dialect, id, args);
    }

    public static String getProperty(String suffix, Class<?> clazz, String dialect, String id, Object... args) {
        String name = clazz.getName().replaceAll("[.]", "/");
        String dialectName = Strings.join(true, ".", new Object[] { Strings.join(true, "$", new Object[] { name, dialect }), suffix });
        /* 缓存配置 */
        Properties properties = CACHE_PROPERTIES.get(dialectName);
        if (properties == null) {
            properties = Helpers.getProperties(clazz, Strings.join(true, ".", new Object[] { name, suffix })); // 通用配置
            /* 方言配置 */
            if (Strings.isNotEmpty(dialect)) {
                properties = Helpers.load(properties, clazz, dialectName);
            }
            // 配置文件中必须包含配置语句
            Asserts.throwState(properties != null, "Not found the [%s] file", dialectName);
            CACHE_PROPERTIES.put(dialectName, properties);
        }
        /* 配置属性 */
        String value = properties.getProperty(id);
        if (Strings.isNotEmpty(value)) {
            value = Strings.format(value, args);
        }
        return value;
    }

    public static int getOrder(Object instance) {
        Integer order;
        getOrder: {
            Annotation[] annos;
            if (instance instanceof Class) {
                annos = ((Class<?>) instance).getDeclaredAnnotations();
            } else if (instance instanceof Method) {
                annos = ((Method) instance).getDeclaredAnnotations();
            } else if (instance != null) {
                annos = instance.getClass().getDeclaredAnnotations();
            } else {
                order = Order.LOW_PRECEDENCE;
                break getOrder;
            }
            order = getOrder(annos);
        }
        return order;
    }

    public static int getOrder(Annotation[] annos) {
        Integer order;
        getOrder: {
            if (annos != null) {
                for (Annotation anno : annos) {
                    order = getOrder(anno);
                    if (order != null) {
                        break getOrder;
                    }
                }
            }
            order = Order.LOW_PRECEDENCE;
        }
        return order;
    }

    private static Integer getOrder(Annotation anno) {
        Integer order = null;
        if (anno instanceof Order) {
            order = ((Order) anno).value();
        }
        return order;
    }

    public static boolean hasAliasName(AnnotatedElement element) {
        return Strings.isNotEmpty(getAliasName(element.getAnnotation(Alias.class))) || Strings.isNotEmpty(getAliasName(element.getAnnotation(javax.inject.Named.class)));
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

    public static String getAliasName(Class<?> clazz, String mask, String value) {
        return getAliasName(clazz, mask, value, clazz.getSimpleName(), Helpers.wraps("i_", 1));
    }

    public static String getAliasName(Method method) {
        return getAliasName(method, null, null);
    }

    public static String getAliasName(Method method, String mask) {
        return getAliasName(method, mask, null);
    }

    public static String getAliasName(Method method, String mask, String value) {
        return getAliasName(method, mask, value, method.getName(), Helpers.wraps("set_|get_", 3), Helpers.wraps("is_", 2));
    }

    private static String getAliasName(AnnotatedElement element, String mask, String value, String name, Object[]... covers) {
        if (hasAliasName(element)) {
            name = Strings.empty(getAliasName(element.getAnnotation(Alias.class)), getAliasName(element.getAnnotation(javax.inject.Named.class)));
        } else if (Strings.isNotEmpty(value)) {
            name = value.trim();
        } else {
            for (Object[] cover : covers) {
                if (Strings.startWithIgnoreCase(Strings.toColumnName(name), (String) cover[0])) {
                    name = name.substring((Integer) cover[1]);
                    break;
                }
            }
            name = Strings.toLower(name);
        }
        return Strings.isEmpty(mask) ? name : Strings.replaceIgnoreCase(name, mask);
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

    public static boolean isEmpty(Map<?, ?> values) {
        return values != null && values.isEmpty();
    }

    public static boolean isEmpty(Collection<?> values) {
        return values != null && values.isEmpty();
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
    public static <T> Map<String, List<T>> toUnmodifiable(Map<String, List<T>> values) {
        for (Entry<String, List<T>> entry : values.entrySet()) {
            values.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
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
    public static Map<String, Object> toHierarchical(Map<String, Object> values) {
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
            index = (key = Strings.toHierarchy(entry.getKey())).indexOf(".");
            if (index == -1) {
                // 没有层级集合
                subKey = null;
            } else {
                // 存在层级集合
                subKey = key.substring(index + 1);
                key = key.substring(0, index);
            }
            value = result.get(key);
            if (value == null) {
                if (subKey == null) {
                    // 没有层级集合，并规范对象
                    result.put(key, (value = entry.getValue()) instanceof Map && value.getClass() != HashMap.class ? new HashMap<String, Object>((Map<String, Object>) value) : value);
                    continue;
                }
                result.put(key, container = new HashMap<String, Object>()); // 保存层级集合
            } else if (value instanceof Map) {
                container = (Map<String, Object>) value; // 获取层级集合
                if (subKey == null) {
                    value = entry.getValue(); // 原始集合对象
                    if (value instanceof Map) {
                        // 合并层级集合
                        String unionKey;
                        Map<String, Object> pending = (Map<String, Object>) value;
                        for (Entry<String, Object> unionEntry : pending.entrySet()) {
                            if (!container.containsKey(unionKey = unionEntry.getKey())) {
                                container.put(unionKey, unionEntry.getValue());
                            }
                        }
                        continue;
                    } else {
                        // 层级对象冲突
                        return values;
                    }
                }
            } else {
                // 层级对象冲突
                return values;
            }
            container.put(subKey, entry.getValue()); // 存放层级对象（该步骤会替换原有层级对象）
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
    public static <K, T> boolean toAppend(Map<K, List<T>> values, K name, T value) {
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
    public static <K, T> boolean toAppend(Map<K, List<T>> values, K name, T value, boolean repeat) {
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
    public static <K, T> int toAppends(Map<K, List<T>> values, K name, Collection<T> value) {
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
    public static <K, T> int toAppends(Map<K, List<T>> values, K name, Collection<T> value, boolean repeat) {
        List<T> container = values.get(name);
        if (container == null) {
            values.put(name, container = new ArrayList<T>());
        }
        return repeat ? (container.addAll(value) ? value.size() : 0) : add(container, value);
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
        return add(container, newList(values));
    }

    public static <T> int add(Collection<T> container, Iterator<T> values) {
        return add(container, newList(values));
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

    public static <T> List<T> newList(Enumeration<T> values) {
        List<T> result = new ArrayList<T>();
        if (values != null) {
            while (values.hasMoreElements()) {
                result.add(values.nextElement());
            }
        }
        return result;
    }

    public static <T> List<T> newList(Iterator<T> values) {
        List<T> result = new ArrayList<T>();
        if (values != null) {
            while (values.hasNext()) {
                result.add(values.next());
            }
        }
        return result;
    }

    public static <K, V> Map<K, V> newMap(Map<K, V> source) {
        Map<K, V> target = new HashMap<K, V>();
        if (source != null) {
            target.putAll(source);
        }
        return target;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Map) {
            params = (Map<String, Object>) value;
        } else if (Strings.isNotEmpty(key)) {
            params = Helpers.toHierarchical(params);
            for (String name : Strings.toHierarchy(key).split("[./]")) {
                value = params.get(name);
                if (value instanceof Map) {
                    params = Helpers.toHierarchical((Map<String, Object>) value);
                } else {
                    params = null;
                    break;
                }
            }
        }
        return params;
    }

    public static Object getInstance(Map<String, Object> params, String key) {
        int index = key.lastIndexOf(".");
        if (index > 0) {
            params = getMap(params, key.substring(0, index));
            key = key.substring(index + 1);
        }
        return params.get(key);
    }

    public static Object toArray(Object source) {
        if (source == null) {
            return null;
        } else if (source instanceof Collection) {
            Collection<?> collection = (Collection<?>) source;
            int size = collection.size();
            Class<?> sourceClass = Object.class;
            if (size > 0) {
                for (Object value : (Collection<?>) source) {
                    if (sourceClass.isInstance(value)) {
                        sourceClass = value.getClass();
                    } else if (value != null) {
                        sourceClass = Object.class;
                        break;
                    }
                }
            }
            return sourceClass == Object.class ? collection.toArray() : toArray(sourceClass, source);
        }
        Class<?> sourceClass = source.getClass();
        if (sourceClass.isArray()) {
            return source;
        } else {
            Object result = Array.newInstance(sourceClass, 1);
            Array.set(result, 0, source);
            return result;
        }
    }

    public static Object toArray(final Class<?> type, Object source) {
        if (source == null) {
            return null;
        }
        final List<Object> container = new ArrayList<Object>();
        each(source, new Callable.Runnable() {
            public void run(Object... args) {
                if (type.isInstance(args[1])) {
                    container.add(args[1]);
                } else if (args[1] == null) {
                    if (type.isPrimitive()) {
                        args[1] = Converter.defaultValue(type);
                    }
                    container.add(args[1]);
                }
            }
        });
        int size = container.size();
        source = Array.newInstance(type, size);
        if (size > 0) {
            System.arraycopy(container.toArray(), 0, source, 0, size);
        }
        return source;
    }

    public static Object newArray(Object source) {
        return newArray(source, -1);
    }

    public static Object newArray(Object source, int length) {
        Class<?> sourceClass = source == null ? Object.class : source.getClass();
        if ((source = toArray(source)) == null) {
            return length < 0 ? null : new Object[length];
        } else if (sourceClass.isArray()) {
            int sourceLength = Array.getLength(source);
            if (length < 0) {
                length = sourceLength;
            } else if (length < sourceLength) {
                sourceLength = length;
            }
            Object target = Array.newInstance(sourceClass.getComponentType(), length);
            if (sourceLength > 0) {
                System.arraycopy(source, 0, target, 0, sourceLength);
            }
            source = target;
        }
        return source;
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
            Object result = Array.newInstance(resultType, sourceCount + targetCount);
            for (Object[] current : new Object[][] { { sourceType, source, 0, sourceCount }, { targetType, target, sourceCount, targetCount } }) {
                if (resultType.isAssignableFrom((Class<?>) current[0])) {
                    System.arraycopy(current[1], 0, result, (Integer) current[2], (Integer) current[3]);
                } else {
                    for (int i = 0; i < (Integer) current[3]; i++) {
                        Array.set(result, i + (Integer) current[2], Array.get(current[1], i));
                    }
                }
            }
            return result;
        }
        return newArray(resultType, source, sourceCount, target, targetCount);
    }

    public static Object newArray(Class<?> resultType, Object source, int sourceCount, Object target, int targetCount) {
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
            Array.set(result, amount, newArray(Array.get(source, amount), Array.get(target, amount)));
        } while (++amount < loopCount);
        while (amount < resultCount) {
            Array.set(result, amount, Array.get(overValue, amount));
            amount++;
        }
        return result;
    }

    public static Object newArray(Class<?> resultType, Object source, int sourceCount) {
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
                for (int j = (result instanceof Object[]) ? ((Object[]) result).length : 1; j < grid.size(); j++) {
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
                int count = (result instanceof Object[]) ? ((Object[]) result).length : 1;
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
                    value = container.size() < 2 ? compact(container) : container.toArray();
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
        if (ms > 0 || ns > 0) {
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
        InetAddress address = null;
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            // 遍历网络接口
            while (ifaces.hasMoreElements()) {
                address = getAddress(ifaces.nextElement(), address);
                if (address != null && address.isSiteLocalAddress()) {
                    return address;
                }
            }
            // 默认本机地址
            if (address == null) {
                address = InetAddress.getLocalHost();
            }
        } catch (Exception e) {
        }
        return address;
    }

    public static InetAddress getAddress(String host) {
        InetAddress address = null;
        try {
            address = getAddress(NetworkInterface.getByInetAddress(InetAddress.getByName(host)), getAddress());
        } catch (Exception e) {
        }
        return address;
    }

    private static InetAddress getAddress(NetworkInterface iface, InetAddress candidateAddress) {
        // 遍历接口IP地址
        for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
            InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
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
        return candidateAddress;
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
        InetAddress address = getAddress(host);
        return address == null ? null : new InetSocketAddress(address, port);
    }

    public static boolean isCode(String value) {
        return isCode(value, 64);
    }

    public static boolean isCode(String value, int limit) {
        if (limit <= 0) {
            limit = 64;
        }
        return Strings.isNotEmpty(value) && value.toUpperCase().matches(REGEX_CODE) && value.length() < limit;
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
    public static <T extends Object> Object each(Object instance, Callable<T> callable, Object... args) {
        return each(false, instance, callable, args);
    }

    /**
     * 遍历操作
     * 
     *
     * @param multipled
     * @param array
     * @param callable
     * @param args
     * @return
     */
    public static <T extends Object> Object each(boolean multipled, Object array, Callable<T> callable, Object... args) {
        boolean isMultiple = true;
        int size;
        if (array instanceof Collection) {
            Collection<?> collection = (Collection<?>) array;
            size = collection.size();
            array = collection.toArray(new Object[size]);
        } else if (array != null && array.getClass().isArray()) {
            size = Array.getLength(array);
        } else {
            isMultiple = false;
            size = 1;
            array = new Object[] { array };
        }
        Object[] result = new Object[size];
        for (int i = 0; i < size; i++) {
            result[i] = callable.call(i, Array.get(array, i), args);
        }
        return isMultiple || multipled ? result : result[0];
    }

    public static int getLength(Object array) {
        int size;
        if (array instanceof Collection) {
            size = ((Collection<?>) array).size();
        } else if (array instanceof Map) {
            size = ((Map<?, ?>) array).size();
        } else if (array == null) {
            size = 0;
        } else if (array.getClass().isArray()) {
            size = Array.getLength(array);
        } else {
            size = 1;
        }
        return size;
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

    public static Object getValue(Map<String, Object> params, String key) {
        return getValue(params, key, (String) null);
    }

    public static Object getValue(Map<String, Object> params, String key, String suffix) {
        suffix = Strings.empty(suffix);
        if ((key = Strings.empty(key)).length() > 0) {
            String pendingKey = Strings.toHierarchy(key); // 层级化
            int index = pendingKey.lastIndexOf(".");
            Map<String, Object> pending;
            if (index > 0) {
                pending = getMap(params, pendingKey.substring(0, index));
                pendingKey = pendingKey.substring(index + 1);
            } else {
                pending = toHierarchical(params);
                pendingKey = key;
            }
            if (pending != null) {
                Object value = pending.get(pendingKey + suffix);
                if (value != null) {
                    return value;
                }
            }
        }
        return params.get(key + suffix);
    }

    @SuppressWarnings("unchecked")
    public static Object getValue(Map<String, Object> params, String key, Class<?> rawClass) {
        if (Reflects.isGeneral(rawClass) || rawClass.isInterface()) {
            return getValue(params, key);
        }
        params = Helpers.toHierarchical(params); // 集合层级化（做为阻止闭环依赖导致无限递归的依据）
        key = Strings.empty(key);
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, List<BoundField>> fields = Reflects.getBoundFields(rawClass);
        String name;
        Object value;
        Object subValue;
        BoundField field;
        Map<String, Object> pending;
        for (Entry<String, List<BoundField>> entry : fields.entrySet()) {
            field = entry.getValue().get(0);
            // 获取字段值（层级化后的集合如有子级必定存在父级）
            if (((value = getValue(params, key + "." + (name = entry.getKey()))) != null) || (Strings.isNotEmpty(name = field.getAlias()) && (value = getValue(params, key + "." + name)) != null)) {
                // 递归获取子字段类型
                if (value instanceof Map) {
                    value = pending = new HashMap<String, Object>((Map<String, Object>) value); // 采用子集合作为容器做递归获取
                    if ((subValue = getValue(pending, name, field.getToken().getRawClass())) != null) {
                        pending.put(name, subValue);
                    }
                }
            } else {
                continue; // 目标值不存在
            }
            result.put(name, value);
        }
        return result.size() > 0 ? result : null;

    }

    public static Object getValues(Map<String, Object> params, String key) {
        return getValues(params, key, (String) null);
    }

    public static Object getValues(Map<String, Object> params, String key, String suffix) {
        Object value = getValue(params, (key = Strings.empty(key)) + "[]", suffix);
        if (value == null) {
            Object pending;
            int amount = 0;
            List<Object> container = new ArrayList<Object>();
            while ((pending = getValue(params, key + "[" + (amount++) + "]", suffix)) != null) {
                container.add(pending);
            }
            if (container.size() > 0) {
                return Helpers.newArray(container.get(0).getClass(), container.toArray(), container.size());
            } else {
                value = getValue(params, key, suffix);
            }
        }
        return Helpers.newArray(value);
    }

    public static Object getValues(Map<String, Object> params, String key, Class<?> rawClass) {
        Object value = getValue(params, (key = Strings.empty(key)) + "[]", rawClass);
        if (value == null) {
            Object pending;
            int amount = 0;
            List<Object> container = new ArrayList<Object>();
            while ((pending = getValue(params, key + "[" + (amount++) + "]", rawClass)) != null) {
                container.add(pending);
            }
            if (container.size() > 0) {
                return Helpers.newArray(container.get(0).getClass(), container.toArray(), container.size());
            } else {
                value = getValue(params, key, rawClass);
            }
        }
        return Helpers.newArray(value);
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

    public static int toInt(int radix, String value, int beginIndex, int endIndex) {
        return Integer.parseInt(value.substring(beginIndex, endIndex), radix);
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

    public static <T> List<T> sort(List<T> instances) {
        Collections.sort(instances, new Comparator<T>() {
            public int compare(T a, T b) {
                return Integer.compare(getOrder(a), getOrder(b));
            }
        });
        return instances;
    }

}

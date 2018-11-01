package net.isger.util.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import net.isger.util.Asserts;
import net.isger.util.Callable;
import net.isger.util.Reflects;
import net.isger.util.Strings;
import net.isger.util.hitch.Director;
import net.isger.util.reflect.conversion.Conversion;

public class Converter {

    private static final String KEY_CONVERSIONS = "brick.util.reflect.conversions";

    private static final String CONVERSION_PATH = "net/isger/util/reflect/conversion";

    private static final Logger LOG;

    private static final Converter CONVERTER;

    private Map<String, Conversion> conversions;

    static {
        LOG = LoggerFactory.getLogger(Converter.class);
        CONVERTER = new Converter();
        new Director() {
            protected String directPath() {
                return directPath(KEY_CONVERSIONS, CONVERSION_PATH);
            }
        }.direct(CONVERTER);
    }

    private Converter() {
        conversions = new Hashtable<String, Conversion>();
    }

    public static boolean contains(Conversion conversion) {
        return CONVERTER.conversions.containsValue(conversion);
    }

    public static void addConversion(Conversion conversion) {
        if (contains(conversion)) {
            return;
        }
        String name = conversion.getClass().getName();
        if (LOG.isDebugEnabled()) {
            LOG.info("Achieve conversion [{}]", conversion);
        }
        conversion = CONVERTER.conversions.put(name, conversion);
        if (conversion != null && LOG.isDebugEnabled()) {
            LOG.warn("(!) Discard conversion [{}]", conversion);
        }
    }

    public static Conversion getConversion(String name) {
        return CONVERTER.conversions.get(name);
    }

    /**
     * 类型检测
     * 
     * @param clazz
     * @return
     */
    public static boolean isSupport(Class<?> clazz) {
        for (Conversion conversion : CONVERTER.conversions.values()) {
            if (conversion.isSupport(clazz)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 转换
     * 
     * @param type
     * @param value
     * @return
     */
    public static Object convert(Type type, Object value) {
        return convert(type, value, null);
    }

    /**
     * 转换
     *
     * @param type
     * @param value
     * @param assembler
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Object convert(Type type, Object value,
            Callable<?> assembler) {
        /* 默认值转换 */
        if (value == null) {
            return defaultValue(type);
        }
        Class<?> rawClass = Reflects.getRawClass(type);
        /* 自定义转换 */
        for (Conversion conversion : CONVERTER.conversions.values()) {
            if (conversion.isSupport(type)) {
                try {
                    return conversion.convert(type, value);
                } catch (Exception e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.warn("Failure to convert [{}] to [{}]", value,
                                rawClass, e);
                    }
                }
            }
        }
        /* 可赋值操作 */
        Class<?> srcClass = value.getClass();
        if (rawClass.isAssignableFrom(srcClass)) {
            return value;
        }
        /* 多值转换 */
        if (value instanceof Collection) {
            value = ((Collection<?>) value).toArray();
            srcClass = value.getClass();
        }
        if ((!(rawClass.isArray()
                || Collection.class.isAssignableFrom(rawClass)))
                && srcClass.isArray()) {
            if (Array.getLength(value) == 0) {
                return defaultValue(rawClass);
            }
            return convert(rawClass, Array.get(value, 0));
        }
        /* 字符串转换 */
        if (value instanceof String) {
            if (Strings.isEmpty(value)) {
                return defaultValue(type);
            }
            Class<?> clazz = Reflects.getClass((String) value);
            if (clazz != null) {
                return Reflects.newInstance(clazz);
            }
            value = new Gson().fromJson((String) value, Object.class);
        }
        /* 字符串赋值 */
        if (rawClass == String.class) {
            return new Gson().toJson(value);
        }
        /* 键值对转换 */
        if (value instanceof Map) {
            Map<String, Object> config = (Map<String, Object>) value;
            if (!config.containsKey(Reflects.KEY_CLASS)) {
                config = new HashMap<String, Object>(config);
                config.put(Reflects.KEY_CLASS, rawClass);
            }
            return Reflects.newInstance(config, assembler);
        }
        throw Asserts.state("Unsupported convert to %s from %s",
                Reflects.getName(rawClass), srcClass.getName());
    }

    public static Object defaultValue(Type type) {
        Class<?> rawClass = Reflects.getRawClass(type);
        if (rawClass.isPrimitive()) {
            if (Boolean.TYPE == rawClass) {
                return false;
            }
            return 0;
        }
        return null;
    }
}

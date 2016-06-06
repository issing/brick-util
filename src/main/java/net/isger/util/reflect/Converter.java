package net.isger.util.reflect;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import net.isger.util.Reflects;
import net.isger.util.hitch.Director;
import net.isger.util.reflect.conversion.Conversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @param clazz
     * @param value
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Object convert(Class<?> clazz, Object value) {
        for (Conversion conversion : CONVERTER.conversions.values()) {
            if (conversion.isSupport(clazz)) {
                try {
                    return conversion.convert(clazz, value);
                } catch (Exception e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.warn("Failure to convert [{}] to [{}]", value,
                                clazz, e);
                    }
                }
            }
        }
        if (value == null) {
            return defaultValue(clazz);
        }
        Class<?> srcType = value.getClass();
        if (clazz.isAssignableFrom(srcType)) {
            return value;
        } else if (clazz.isArray() && srcType.isArray()) {
            // TODO 未处理数组类型
        } else if (srcType.isArray()) {
            if (Array.getLength(value) == 0) {
                return defaultValue(clazz);
            }
            return convert(clazz, Array.get(value, 0));
        } else if (clazz.isArray()) {
            if (clazz.getComponentType().isAssignableFrom(srcType)) {
                Object array = Array.newInstance(clazz.getComponentType(), 1);
                Array.set(array, 0, value);
                return array;
            }
        } else if (value instanceof String) {
            return Reflects.newInstance((String) value);
        } else if (value instanceof Map) {
            Map<String, Object> config = (Map<String, Object>) value;
            if (!config.containsKey(Reflects.KEY_CLASS)) {
                config = new HashMap<String, Object>(config);
                config.put(Reflects.KEY_CLASS, clazz);
            }
            return Reflects.newInstance(config);
        } else if (clazz == String.class) {
            return String.valueOf(value);
        }
        throw new IllegalStateException("Unsupported convert to "
                + clazz.getName() + " from " + srcType.getName());
    }

    public static Object defaultValue(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return 0;
        }
        return null;
    }
}

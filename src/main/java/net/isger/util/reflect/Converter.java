package net.isger.util.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.isger.util.Asserts;
import net.isger.util.Helpers;
import net.isger.util.Reflects;
import net.isger.util.Strings;
import net.isger.util.hitch.Director;
import net.isger.util.reflect.conversion.Conversion;

/**
 * 类型转换器
 * 
 * @author issing
 */
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
        this.conversions = new Hashtable<String, Conversion>();
    }

    /**
     * 包含转换
     * 
     * @param conversion
     * @return
     */
    public static boolean contains(Conversion conversion) {
        return CONVERTER.conversions.containsValue(conversion);
    }

    /**
     * 添加转换
     * 
     * @param conversion
     */
    public static void addConversion(Conversion conversion) {
        if (contains(conversion)) return;
        String name = conversion.getClass().getName();
        if (LOG.isDebugEnabled()) LOG.info("Achieve conversion [{}]", conversion);
        conversion = CONVERTER.conversions.put(name, conversion);
        if (conversion != null && LOG.isDebugEnabled()) LOG.warn("(!) Discard conversion [{}]", conversion);
    }

    /**
     * 获取转换
     * 
     * @param name
     * @return
     */
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
            if (conversion.isSupport(clazz)) return true;
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
    public static Object convert(Type type, Object value, ClassAssembler assembler) {
        /* 默认值转换 */
        Class<?> rawClass = Reflects.getRawClass(type);
        if (value == null || Reflects.getPrimitiveClass(rawClass) == Void.TYPE) return defaultValue(type);
        /* 自定义转换 */
        for (Conversion conversion : CONVERTER.conversions.values()) {
            if (conversion.isSupport(type)) {
                try {
                    return conversion.convert(type, value, assembler);
                } catch (Exception e) {
                    if (LOG.isDebugEnabled()) LOG.warn("Failure to convert [{}] to [{}]", value, rawClass);
                }
            }
        }
        /* 可赋值操作 */
        Class<?> srcClass = value.getClass();
        if (rawClass.isAssignableFrom(srcClass)) return value;
        /* 多值转换 */
        if (value instanceof Collection) {
            value = ((Collection<?>) value).toArray();
            srcClass = value.getClass();
        }
        if ((!(rawClass.isArray() || Collection.class.isAssignableFrom(rawClass))) && srcClass.isArray()) {
            if (Array.getLength(value) == 0) return defaultValue(rawClass);
            return convert(rawClass, Array.get(value, 0));
        }
        /* 字符串转换 */
        if (value instanceof String) {
            if (Strings.isEmpty(value)) return defaultValue(type);
            Class<?> clazz = Reflects.getClass((String) value);
            if (clazz != null) return Reflects.newInstance(clazz, assembler);
            value = Helpers.fromJson((String) value);
        }
        /* 字符串赋值 */
        if (rawClass == String.class) return Helpers.toJson(value).replaceFirst("^[\"]+", "").replaceFirst("[\"]$", "");
        /* 键值对转换 */
        if (value instanceof Map) return Reflects.newInstance(rawClass, (Map<String, Object>) value, assembler);
        /* 不支持转换 */
        throw Asserts.state("Unsupported convert to %s from %s", Reflects.getName(rawClass), srcClass.getName());
    }

    public static Object defaultValue(Type type) {
        Class<?> rawClass = Reflects.getRawClass(type);
        if (rawClass.isPrimitive() && rawClass != Void.TYPE) {
            if (Boolean.TYPE == rawClass) return false;
            return 0;
        }
        return null;
    }
}

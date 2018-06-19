package net.isger.util.load;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.isger.util.Asserts;
import net.isger.util.Reflects;
import net.isger.util.Strings;
import net.isger.util.anno.Ignore;
import net.isger.util.reflect.Converter;

/**
 * 基础加载器
 * 
 * @author issing
 * 
 */
@Ignore
public class BaseLoader implements Loader {

    public static final String PARAM_CLASS = "class";

    private static final Loader LOADER;

    /** 目标类型 */
    private Class<?> targetClass;

    static {
        LOADER = new BaseLoader(Object.class);
    }

    public BaseLoader() {
    }

    public BaseLoader(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    /**
     * 加载资源
     * 
     * @param res
     * @return
     */
    public static Object toLoad(Object res) {
        try {
            return LOADER.load(res);
        } catch (Exception e) {
            return res;
        }
    }

    /**
     * 目标类型
     * 
     * @return
     */
    public Class<?> getTargetClass() {
        return this.targetClass;
    }

    /**
     * 实现类型
     * 
     * @return
     */
    public Class<?> getImplementClass() {
        return getTargetClass();
    }

    /**
     * 实现类型
     * 
     * @param res
     * @return
     */
    public Class<?> getImplementClass(Map<String, Object> res) {
        Class<?> clazz;
        Class<?> targetClass = this.getTargetClass();
        String className;
        if (res != null && Strings
                .isNotEmpty(className = (String) res.get(PARAM_CLASS))) {
            /* 使用配置实现类 */
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(
                        "Not found class " + className);
            }
        } else {
            clazz = getImplementClass();
        }
        // 指定目标类型约束检测
        Asserts.isAssignable(targetClass, clazz);
        return clazz;
    }

    /**
     * 加载资源
     * 
     */
    @SuppressWarnings("unchecked")
    public final Object load(Object res) {
        Object result;
        if (res instanceof String) {
            /* 字符串资源方式加载 */
            result = load((String) res);
        } else if (res instanceof Collection) {
            /* 集合资源方式加载 */
            result = load((Collection<?>) res);
        } else if (res instanceof Map) {
            /* 键值对资源方式加载 */
            result = load((Map<String, Object>) res);
        } else {
            /* 默认创建实例 */
            result = create(res);
        }
        return result;
    }

    /**
     * 加载资源
     * 
     * @param res
     * @return
     */
    protected Object load(String res) {
        return create(res);
    }

    /**
     * 加载资源
     * 
     * @param res
     * @return
     */
    protected Object load(Collection<?> res) {
        List<Object> result = new ArrayList<Object>();
        Object instance;
        for (Object config : res) {
            // 阻止列表集合无限嵌套
            instance = config instanceof Collection ? create(config)
                    : load(config);
            if (instance instanceof Collection) {
                result.addAll((Collection<?>) instance);
            } else {
                result.add(instance);
            }
        }
        return result;
    }

    /**
     * 加载资源
     * 
     * @param res
     * @return
     */
    protected Object load(Map<String, Object> res) {
        return create(getImplementClass(res), res);
    }

    /**
     * 创建实例
     * 
     * @param clazz
     * @param res
     * @return
     */
    protected Object create(Class<?> clazz, Map<String, Object> res) {
        return clazz == Object.class ? res : Reflects.newInstance(clazz, res);
    }

    /**
     * 创建实例
     * 
     * @param res
     * @return
     */
    protected Object create(Object res) {
        Class<?> implementClass = getImplementClass();
        // 指定目标类型约束检测
        Asserts.isAssignable(getTargetClass(), implementClass);
        return Converter.convert(implementClass, res);
    }

}

package net.isger.util.load;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.isger.util.Asserts;
import net.isger.util.Reflects;
import net.isger.util.Strings;
import net.isger.util.anno.Ignore;
import net.isger.util.reflect.ClassAssembler;
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
        return toLoad(res, null);
    }

    /**
     * 加载资源
     *
     * @param res
     * @param assembler
     * @return
     */
    public static Object toLoad(Object res, ClassAssembler assembler) {
        try {
            return LOADER.load(res, assembler);
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
        if (res != null && Strings.isNotEmpty(className = (String) res.get(PARAM_CLASS))) {
            /* 使用配置实现类 */
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw Asserts.argument("Not found class %s", className);
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
    public final Object load(Object res, ClassAssembler assembler) {
        Object result;
        if (res instanceof String) {
            /* 字符串资源方式加载 */
            result = load((String) res, assembler);
        } else if (res instanceof Collection) {
            /* 集合资源方式加载 */
            result = load((Collection<?>) res, assembler);
        } else if (res instanceof Map) {
            /* 键值对资源方式加载 */
            result = load((Map<String, Object>) res, assembler);
        } else {
            /* 默认创建实例 */
            result = create(res, assembler);
        }
        return result;
    }

    /**
     * 加载资源
     * 
     * @param res
     * @return
     */
    protected Object load(String res, ClassAssembler assembler) {
        return create(res, assembler);
    }

    /**
     * 加载资源
     * 
     * @param res
     * @return
     */
    protected Object load(Collection<?> res, ClassAssembler assembler) {
        List<Object> result = new ArrayList<Object>();
        Object instance;
        for (Object config : res) {
            // 阻止列表集合无限嵌套
            instance = config instanceof Collection ? create(config, assembler) : load(config, assembler);
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
    protected Object load(Map<String, Object> res, ClassAssembler assembler) {
        return create(getImplementClass(res), res, assembler);
    }

    /**
     * 创建实例
     * 
     * @param clazz
     * @param res
     * @return
     */
    protected Object create(Class<?> clazz, Map<String, Object> res, ClassAssembler assembler) {
        return clazz == Object.class ? res : Reflects.newInstance(clazz, res, assembler);
    }

    /**
     * 创建实例
     * 
     * @param res
     * @return
     */
    protected Object create(Object res, ClassAssembler assembler) {
        Class<?> implementClass = getImplementClass();
        // 指定目标类型约束检测
        Asserts.isAssignable(getTargetClass(), implementClass);
        return Converter.convert(implementClass, res, assembler);
    }

}

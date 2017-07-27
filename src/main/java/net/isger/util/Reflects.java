package net.isger.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.isger.util.anno.Ignore;
import net.isger.util.anno.Ignore.Mode;
import net.isger.util.reflect.BoundField;
import net.isger.util.reflect.BoundMethod;
import net.isger.util.reflect.Constructor;

/**
 * 反射工具
 * 
 * @author issing
 * 
 */
public class Reflects {

    /** 反射类配置键 */
    public static final String KEY_CLASS = "class";

    /** 包装类型集合 */
    private static final Map<Class<?>, Class<?>> WRAP_TYPES;

    private static final Logger LOG;

    /** 类字段缓存 */
    private static final Map<Class<?>, Map<String, List<BoundField>>> FIELDS;

    /** 类方法缓存 */
    private static final Map<Class<?>, Map<String, List<BoundMethod>>> METHODS;

    static {
        WRAP_TYPES = new HashMap<Class<?>, Class<?>>();
        WRAP_TYPES.put(Void.TYPE, Void.class);
        WRAP_TYPES.put(Boolean.TYPE, Boolean.class);
        WRAP_TYPES.put(Byte.TYPE, Byte.class);
        WRAP_TYPES.put(Character.TYPE, Character.class);
        WRAP_TYPES.put(Short.TYPE, Short.class);
        WRAP_TYPES.put(Integer.TYPE, Integer.class);
        WRAP_TYPES.put(Long.TYPE, Long.class);
        WRAP_TYPES.put(Float.TYPE, Float.class);
        WRAP_TYPES.put(Double.TYPE, Double.class);

        LOG = LoggerFactory.getLogger(Reflects.class);

        FIELDS = new ConcurrentHashMap<Class<?>, Map<String, List<BoundField>>>();
        METHODS = new ConcurrentHashMap<Class<?>, Map<String, List<BoundMethod>>>();
    }

    private Reflects() {
    }

    /**
     * 获取包装类型
     * 
     * @param primitive
     * @return
     */
    public static Class<?> getWrapClass(Class<?> primitive) {
        Class<?> wrap = WRAP_TYPES.get(primitive);
        if (wrap == null) {
            wrap = primitive;
        }
        return wrap;
    }

    /**
     * 根据资源名称获取输入流
     * 
     * @param name
     * @return
     */
    public static InputStream getResourceAsStream(String name) {
        ClassLoader classLoader = getClassLoader();
        InputStream stream = classLoader.getResourceAsStream(name);
        if (stream == null) {
            stream = Reflects.class.getClassLoader().getResourceAsStream(name);
        }
        return stream;
    }

    /**
     * 获取绑定字段信息
     * 
     * @param clazz
     * @return
     */
    public static Map<String, List<BoundField>> getBoundFields(Class<?> clazz) {
        Map<String, List<BoundField>> result = FIELDS.get(clazz);
        // 跳过接口以及原始数据类型（不继承Object类）
        if (result != null || clazz.isInterface()
                || !Object.class.isAssignableFrom(clazz)) {
            return result;
        }
        result = new LinkedHashMap<String, List<BoundField>>();
        String name;
        Mode ignoreMode;
        BoundField boundField;
        List<BoundField> boundFields;
        Class<?> type = clazz;
        while (type != null && type != Object.class) {
            // 忽略指定类
            ignoreMode = getIgnoreMode(type);
            // 导入声明字段（字段名优先）
            boundFields = new ArrayList<BoundField>();
            for (Field field : type.getDeclaredFields()) {
                if ((boundField = createBoundField(field, ignoreMode)) != null
                        && toAppend(result, boundField.getName(), boundField)) {
                    boundFields.add(boundField);
                }
            }
            // 导入别名字段（候选）
            for (BoundField field : boundFields) {
                name = field.getAlias();
                if (name != null) {
                    toAppend(result, name, field);
                }
            }
            type = type.getSuperclass();
        }
        // 屏蔽修改
        FIELDS.put(clazz, result = toUnmodifiable(result));
        return result;
    }

    /**
     * 获取绑定字段信息
     * 
     * @param clazz
     * @param fieldName
     * @return
     */
    public static BoundField getBoundField(Class<?> clazz, String fieldName) {
        Map<String, List<BoundField>> boundFields = getBoundFields(clazz);
        List<BoundField> bounds = boundFields.get(fieldName);
        if (bounds != null) {
            return bounds.get(0);
        }
        return null;
    }

    /**
     * 创建绑定字段信息
     * 
     * @param field
     * @param mode
     * @return
     */
    private static BoundField createBoundField(Field field, Mode mode) {
        int mod = field.getModifiers();
        // 忽略静态、终态、暂态、瞬态
        if (Modifier.isStatic(mod) || Modifier.isFinal(mod)
                || Modifier.isTransient(mod) || Modifier.isVolatile(mod)) {
            return null;
        }
        Ignore ignore = field.getAnnotation(Ignore.class);
        if (Mode.EXCLUDE.equals(Helpers.getMode(ignore, mode))) {
            return null;
        }
        return new BoundField(field);
    }

    /**
     * 获取绑定方法信息
     * 
     * @param clazz
     * @return
     */
    public static Map<String, List<BoundMethod>> getBoundMethods(
            Class<?> clazz) {
        Map<String, List<BoundMethod>> result = METHODS.get(clazz);
        // 跳过接口以及原始数据类型（不继承Object类）
        if (result != null || !Object.class.isAssignableFrom(clazz)) {
            return result;
        }
        result = new LinkedHashMap<String, List<BoundMethod>>();
        String name;
        Mode ignoreMode;
        BoundMethod boundMethod;
        List<BoundMethod> boundMethods;
        Class<?> type = clazz;
        while (type != null && type != Object.class) {
            // 忽略指定类
            ignoreMode = getIgnoreMode(type);
            // 导入声明方法（方法名优先）
            boundMethods = new ArrayList<BoundMethod>();
            for (Method method : type.getDeclaredMethods()) {
                if ((boundMethod = createBoundMethod(method,
                        ignoreMode)) != null
                        && toAppend(result, boundMethod.getName(), boundMethod)
                        && toAppend(result, boundMethod.getMethodDesc(),
                                boundMethod)) {
                    boundMethods.add(boundMethod);
                }
            }
            // 导入别名及描述名方法（候选）
            for (BoundMethod method : boundMethods) {
                name = method.getAliasName();
                if (name != null) {
                    toAppend(result, name, method);
                }
                toAppend(result, method.getMethodDesc(), method);
            }
            type = type.getSuperclass();
        }
        // 屏蔽修改
        METHODS.put(clazz, result = toUnmodifiable(result));
        return result;
    }

    /**
     * 获取绑定方法信息
     * 
     * @param clazz
     * @param name
     * @return
     */
    public static BoundMethod getBoundMethod(Class<?> clazz, String name) {
        List<BoundMethod> bounds = getBoundMethods(clazz).get(name);
        return bounds == null ? null : bounds.get(0);
    }

    /**
     * 创建绑定方法信息
     * 
     * @param method
     * @param mode
     * @return
     */
    private static BoundMethod createBoundMethod(Method method, Mode mode) {
        int mod = method.getModifiers();
        // 忽略静态
        if (Modifier.isStatic(mod)) {
            // || Modifier.isAbstract(mod)) {
            return null;
        }
        Ignore ignore = method.getAnnotation(Ignore.class);
        if (Mode.EXCLUDE.equals(Helpers.getMode(ignore, mode))) {
            return null;
        }
        return new BoundMethod(method);
    }

    /**
     * 获取忽略模式
     * 
     * @param clazz
     * @return
     */
    private static Mode getIgnoreMode(Class<?> clazz) {
        Ignore ignore = clazz.getAnnotation(Ignore.class);
        Mode mode = Helpers.getMode(ignore, null);
        if (mode != null) {
            return mode;
        }
        /* 忽略配置文件（TODO 忽略配置规则不完善） */
        String path = clazz.getName().replaceAll("[.]", "/");
        String name = clazz.getSimpleName();
        Properties props = new Properties();
        load(props, path.substring(0, path.length() - name.length())
                + ".ignoreMode");
        load(props, path + ".ignoreMode");
        String modeName = props.getProperty("this");
        if (Mode.EXCLUDE_NAME.equals(modeName)) {
            mode = Mode.EXCLUDE;
        } else {
            mode = Mode.INCLUDE;
        }
        return mode;
    }

    /**
     * 屏蔽集合修改功能
     * 
     * @param values
     * @return
     */
    private static <T> Map<String, List<T>> toUnmodifiable(
            Map<String, List<T>> values) {
        for (Entry<String, List<T>> entry : values.entrySet()) {
            values.put(entry.getKey(),
                    Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(values);
    }

    /**
     * 追加实例
     * 
     * @param values
     * @param name
     * @param value
     * @return
     */
    private static <T> boolean toAppend(Map<String, List<T>> values,
            String name, T value) {
        List<T> list = values.get(name);
        if (list == null) {
            list = new ArrayList<T>();
            values.put(name, list);
        }
        return list.add(value);
    }

    private static void load(Properties props, String path) {
        InputStream is = getResourceAsStream(path);
        if (is != null) {
            try {
                props.load(is);
            } catch (IOException e) {
            }
        }
    }

    /**
     * 集合填充生成指定类型对象
     * 
     * @param name
     * @param values
     * @return
     */
    public static Object newInstance(Map<String, Object> values) {
        Object type = values.get(KEY_CLASS);
        if (type == null) {
            return values;
        }
        values.remove(KEY_CLASS);
        Class<?> clazz = type instanceof Class ? (Class<?>) type
                : getClass((String) type);
        Asserts.isNotNull(clazz, "Cannot instantiation type " + type);
        return newInstance(clazz, values);
    }

    /**
     * 集合填充生成指定类型对象
     * 
     * @param name
     * @param values
     * @return
     */
    public static Object newInstance(String name, Map<String, Object> values) {
        Object instance = newInstance(name);
        toInstance(instance, values);
        return instance;
    }

    /**
     * 集合填充生成指定类型对象
     * 
     * @param clazz
     * @param values
     * @return
     */
    public static <T> T newInstance(Class<T> clazz,
            Map<String, Object> values) {
        T instance = newInstance(clazz);
        if (values != null && values.size() > 0) {
            toInstance(instance, values);
        }
        return instance;
    }

    /**
     * 集合填充生成指定类型对象
     * 
     * @param params
     * @param namespace
     * @return
     */
    public static Object newInstance(Map<String, Object> params,
            String namespace) {
        return newInstance(Helpers.getMap(params, namespace));
    }

    /**
     * 集合填充生成指定类型对象
     * 
     * @param clazz
     * @param params
     * @param namespace
     * @return
     */
    public static <T> T newInstance(Class<T> clazz, Map<String, Object> params,
            String namespace) {
        return Reflects.newInstance(clazz, Helpers.getMap(params, namespace));
    }

    /**
     * 生成实例
     * 
     * @param name
     * @return
     */
    public static Object newInstance(String name) {
        Class<?> clazz = getClass(name);
        Asserts.isNotNull(clazz, "Cannot instantiation type " + name);
        return newInstance(clazz);
    }

    /**
     * 生成实例
     * 
     * @param clazz
     * @return
     */
    public static <T> T newInstance(Class<? extends T> clazz) {
        if (isAbstract(clazz)) {
            // return new Standin(clazz).getSource();
            throw new IllegalStateException("Unsupport " + clazz);
        }
        return Constructor.construct(clazz);
    }

    /**
     * 是否为抽象类
     * 
     * @param clazz
     * @return
     */
    public static boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * 是否为抽象方法
     * 
     * @param method
     * @return
     */
    public static boolean isAbstract(Method method) {
        return Modifier.isAbstract(method.getModifiers());
    }

    /**
     * 是否为类
     * 
     * @param name
     * @return
     */
    public static boolean isClass(String name) {
        return getClass(name) != null;
    }

    /**
     * 获取类型
     * 
     * @param instance
     * @return
     */
    public static Class<?> getClass(Object instance) {
        Class<?> clazz = null;
        if (instance instanceof String) {
            clazz = getClass((String) instance);
        } else if (instance instanceof Class) {
            clazz = (Class<?>) instance;
        } else if (instance != null) {
            clazz = instance.getClass();
        }
        return clazz;
    }

    /**
     * 获取类型
     * 
     * @param name
     * @return
     */
    public static Class<?> getClass(String name) {
        return getClass(name, null);
    }

    /**
     * 获取类型
     * 
     * @param name
     * @param classLoader
     * @return
     * @throws ClassNotFoundException
     * @throws LinkageError
     */
    public static Class<?> getClass(String name, ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = getClassLoader();
        }
        Class<?> result = null;
        try {
            result = (classLoader != null ? classLoader.loadClass(name)
                    : Class.forName(name));
        } catch (Exception ex) {
        }
        return result;
    }

    /**
     * 获取类加载器
     * 
     * @param source
     * @return
     */
    public static ClassLoader getClassLoader(Object source) {
        Class<?> clazz;
        if (source == null || (clazz = getClass(source)) == null) {
            return getClassLoader();
        }
        ClassLoader loader = clazz.getClassLoader();
        if (loader == null) {
            loader = getClassLoader();
        }
        return loader;
    }

    /**
     * 获取类加载器
     * 
     * @return
     */
    public static ClassLoader getClassLoader() {
        ClassLoader classLoader;
        try {
            classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader != null) {
                return classLoader;
            }
        } catch (Throwable ex) {
        }
        classLoader = Reflects.class.getClassLoader();
        if (classLoader == null) {
            try {
                classLoader = ClassLoader.getSystemClassLoader();
            } catch (Throwable ex) {
            }
        }
        return classLoader;
    }

    /**
     * 设置实例所有属性值（通过集合数据）
     * 
     * @param type
     * @param values
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T toInstance(T instance, Map<String, Object> values) {
        if (instance instanceof Map) {
            ((Map<String, Object>) instance).putAll(values);
            return instance;
        }
        values = Helpers.canonicalize(values);
        String fieldName = null;
        Map<String, List<BoundField>> fields = getBoundFields(
                instance.getClass());
        for (Entry<String, List<BoundField>> entry : fields.entrySet()) {
            fieldName = entry.getKey();
            if (values.containsKey(fieldName)) {
                entry.getValue().get(0).setValue(instance,
                        values.get(fieldName));
            }
        }
        return instance;
    }

    public static List<URL> getResources(String name) {
        return getResources(null, name);
    }

    public static List<URL> getResources(Object source, String name) {
        Enumeration<URL> urls;
        List<URL> result = new ArrayList<URL>();
        ClassLoader loader = Reflects.getClassLoader(source);
        get: try {
            urls = loader.getResources(name);
            if (urls == null) {
                if (loader == ClassLoader.getSystemClassLoader()) {
                    break get;
                }
                urls = ClassLoader.getSystemClassLoader().getResources(name);
                if (urls == null) {
                    break get;
                }
            }
            while (urls.hasMoreElements()) {
                result.add(urls.nextElement());
            }
        } catch (Exception e) {
        }
        return result;
    }

    public static URL getResource(String name) {
        return getResource(null, name);
    }

    public static URL getResource(Object source, String name) {
        ClassLoader loader = Reflects.getClassLoader(source);
        URL result;
        get: try {
            result = loader.getResource(name);
            if (result == null) {
                if (loader == ClassLoader.getSystemClassLoader()) {
                    break get;
                }
                result = ClassLoader.getSystemClassLoader().getResource(name);
            }
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    /**
     * 设置实例属性值
     * 
     * @param instance
     * @param name
     * @param value
     */
    public static void toField(Object instance, String name, Object value) {
        BoundField field = getBoundField(instance.getClass(), name);
        if (field == null) {
            throw new IllegalStateException("Not found field by " + name);
        }
        field.setValue(instance, value);
    }

    /**
     * 根据网格模型转换为实例
     * 
     * @param clazz
     * @param grid
     * @return
     */
    public static <T> T toBean(Class<T> clazz, Object[] grid) {
        Object[] values = grid[1] instanceof Object[][]
                ? ((Object[][]) grid[1])[0] : (Object[]) grid[1];
        return toBean(clazz, (Object[]) grid[0], values);
    }

    /**
     * 根据网格模型转换为实例
     * 
     * @param clazz
     * @param columns
     * @param values
     * @return
     */
    public static <T> T toBean(Class<T> clazz, Object[] columns,
            Object[] values) {
        return Reflects.newInstance(clazz, toMap(columns, values));
    }

    /**
     * 提取字段值转换为集合
     * 
     * @param bean
     * @return
     */
    public static Map<String, Object> toMap(Object bean) {
        Map<String, Object> values = new HashMap<String, Object>();
        Map<String, List<BoundField>> fields = getBoundFields(bean.getClass());
        for (Entry<String, List<BoundField>> entry : fields.entrySet()) {
            try {
                values.put(entry.getKey(),
                        entry.getValue().get(0).getValue(bean));
            } catch (Exception e) {
                LOG.warn("Failure getting field [{}] value.", entry.getKey(),
                        e);
            }
        }
        return values;
    }

    /**
     * 根据网格模型转换为集合
     * 
     * @param grid
     * @return
     */
    public static Map<String, Object> toMap(Object[] grid) {
        Object[] values = grid[1] instanceof Object[][]
                ? ((Object[][]) grid[1])[0] : (Object[]) grid[1];
        return toMap((Object[]) grid[0], values);
    }

    /**
     * 根据网格模型转换为集合
     * 
     * @param columns
     * @param values
     * @return
     */
    public static Map<String, Object> toMap(Object[] columns, Object[] values) {
        int size = Math.min(columns.length, values.length);
        Map<String, Object> bean = new HashMap<String, Object>(size);
        for (int i = 0; i < size; i++) {
            bean.put(String.valueOf(columns[i]), values[i]);
        }
        return bean;
    }

    /**
     * 根据网格模型转换为集合
     * 
     * @param clazz
     * @param grid
     * @return
     */
    public static <T> List<T> toList(Class<T> clazz, Object[] grid) {
        List<T> result = new ArrayList<T>();
        Object[] columns = (Object[]) grid[0];
        Object gridValue = grid[1];
        if (gridValue instanceof Object[][]) {
            for (Object[] values : (Object[][]) gridValue) {
                result.add(toBean(clazz, columns, values));
            }
        } else if (gridValue instanceof Object[]) {
            result.add(toBean(clazz, columns, (Object[]) gridValue));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> toList(Class<T> clazz,
            List<Map<String, Object>> values) {
        return toList(clazz, values, new Callable<T>() {
            public T call(Object... args) {
                return (T) args[1];
            }
        });
    }

    public static <T> List<T> toList(Class<T> clazz,
            List<Map<String, Object>> values, Callable<T> callable) {
        List<T> result = new ArrayList<T>(values.size());
        int step = 0;
        for (Map<String, Object> value : values) {
            result.add(
                    callable.call(step++, newInstance(clazz, value), result));
        }
        return result;
    }

    public static List<Map<String, Object>> toList(Object[] columns,
            Object[][] values) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(
                values.length);
        for (Object[] value : values) {
            result.add(toMap(columns, value));
        }
        return result;
    }

    public static List<Map<String, Object>> toList(Object[] grid) {
        return toList((Object[]) grid[0], (Object[][]) grid[1]);
    }

    // public static List<Object> toList(Object[] gridModel, String column) {
    // List<Object> result = new ArrayList<Object>();
    // String[] columns = (String[]) gridModel[0];
    // int size = columns.length;
    // int index;
    // for (index = 0; index < size; index++) {
    // if (column.equals(columns[index])) {
    // break;
    // }
    // }
    // if (index < size) {
    // Object[][] rows = (Object[][]) gridModel[1];
    // for (Object[] row : rows) {
    // result.add(row[index]);
    // }
    // }
    // return result;
    // }
    //
    // public static <T> List<T> toListBean(Class<T> clazz, Object[] values) {
    // List<T> result = new ArrayList<T>();
    // Map<String, Method> methods = getSM(clazz);
    // String[] columns = (String[]) values[0];
    // int size = columns.length;
    // Object[][] datas = (Object[][]) values[1];
    // int count = datas.length;
    // try {
    // Method method = null;
    // for (int i = 0; i < count; i++) {
    // T bean = clazz.newInstance();
    // for (int j = 0; j < size; j++) {
    // method = methods.get(columns[j]);
    // if (method != null) {
    // method.invoke(bean, datas[i][j]);
    // }
    // }
    // result.add(bean);
    // }
    // } catch (Exception e) {
    // throw new RuntimeException(e);
    // }
    // return result;
    // }
    //
    // /**
    // * 获取实例
    // *
    // * @param data
    // * @return
    // */
    // public static Object getInstance(byte[] data) {
    // Object result = null;
    // ByteArrayInputStream buffer = new ByteArrayInputStream(data);
    // ObjectInputStream is = null;
    // try {
    // is = new ObjectInputStream(buffer);
    // result = is.readObject();
    // } catch (Exception e) {
    // throw new RuntimeException(e);
    // } finally {
    // close(is);
    // close(buffer);
    // }
    // return result;
    // }
    //
    // /**
    // * 获取字节码
    // *
    // * @param instance
    // * @return
    // */
    // public static byte[] getBytes(Object instance) {
    // byte[] result = null;
    // ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    // ObjectOutputStream os = null;
    // try {
    // os = new ObjectOutputStream(buffer);
    // os.writeObject(instance);
    // result = buffer.toByteArray();
    // } catch (Exception e) {
    // throw new RuntimeException(e);
    // } finally {
    // close(os);
    // close(buffer);
    // }
    // return result;
    // }

}

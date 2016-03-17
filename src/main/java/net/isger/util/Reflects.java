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

import net.isger.util.anno.Ignore;
import net.isger.util.anno.Ignore.Mode;
import net.isger.util.reflect.BoundField;
import net.isger.util.reflect.BoundMethod;
import net.isger.util.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 反射工具
 * 
 * @author issing
 * 
 */
public class Reflects {

    /** 反射类配置键 */
    public static final String KEY_CLASS = "class";

    private static final Logger LOG;

    /** 类字段缓存 */
    private static final Map<Class<?>, Map<String, List<BoundField>>> FIELDS;

    /** 类方法缓存 */
    private static final Map<Class<?>, Map<String, List<BoundMethod>>> METHODS;

    static {
        LOG = LoggerFactory.getLogger(Reflects.class);
        FIELDS = new ConcurrentHashMap<Class<?>, Map<String, List<BoundField>>>();
        METHODS = new ConcurrentHashMap<Class<?>, Map<String, List<BoundMethod>>>();
    }

    private Reflects() {
    }

    /**
     * 根据资源名称获取输入流
     * 
     * @param name
     * @return
     */
    public static InputStream getResourceAsStream(String name) {
        ClassLoader classLoader = getClassLoader();
        InputStream resourceStream = classLoader.getResourceAsStream(name);
        if (resourceStream == null) {
            classLoader = Reflects.class.getClassLoader();
            resourceStream = classLoader.getResourceAsStream(name);
        }
        return resourceStream;
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
        while (clazz != Object.class) {
            // 忽略指定类
            ignoreMode = getIgnoreMode(clazz);
            // 导入声明字段（字段名优先）
            boundFields = new ArrayList<BoundField>();
            for (Field field : clazz.getDeclaredFields()) {
                if ((boundField = createBoundField(field, ignoreMode)) != null
                        && add(result, boundField.getName(), boundField)) {
                    boundFields.add(boundField);
                }
            }
            // 导入别名字段（候选）
            for (BoundField field : boundFields) {
                name = field.getAliasName();
                if (name != null) {
                    add(result, name, field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        // 屏蔽修改
        FIELDS.put(clazz, result = unmodifiable(result));
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
    public static Map<String, List<BoundMethod>> getBoundMethods(Class<?> clazz) {
        Map<String, List<BoundMethod>> result = METHODS.get(clazz);
        // 跳过接口以及原始数据类型（不继承Object类）
        if (result != null || !Object.class.isAssignableFrom(clazz)) {
            return result;
        }
        METHODS.put(clazz,
                result = new LinkedHashMap<String, List<BoundMethod>>());
        String name;
        Mode ignoreMode;
        BoundMethod boundMethod;
        List<BoundMethod> boundMethods;
        while (clazz != Object.class) {
            // 忽略指定类
            ignoreMode = getIgnoreMode(clazz);
            // 导入声明方法（方法名优先）
            boundMethods = new ArrayList<BoundMethod>();
            for (Method method : clazz.getDeclaredMethods()) {
                if ((boundMethod = createBoundMethod(method, ignoreMode)) != null
                        && add(result, boundMethod.getName(), boundMethod)
                        && add(result, boundMethod.getMethodName(), boundMethod)) {
                    boundMethods.add(boundMethod);
                }
            }
            // 导入别名方法（候选）
            for (BoundMethod method : boundMethods) {
                name = method.getAliasName();
                if (name != null) {
                    add(result, name, method);
                }
            }
            clazz = clazz.getSuperclass();
            if (clazz == null) {
                break;
            }
        }
        return result;
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
    private static <T> Map<String, List<T>> unmodifiable(
            Map<String, List<T>> values) {
        for (Entry<String, List<T>> entry : values.entrySet()) {
            values.put(entry.getKey(),
                    Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(values);
    }

    private static <T> boolean add(Map<String, List<T>> values, String name,
            T value) {
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
    public static <T> T newInstance(Class<T> clazz, Map<String, Object> values) {
        T instance = newInstance(clazz);
        if (values != null && values.size() > 0) {
            toInstance(instance, values);
        }
        return instance;
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
            result = (classLoader != null ? classLoader.loadClass(name) : Class
                    .forName(name));
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
    public static void toInstance(Object instance, Map<String, Object> values) {
        if (instance instanceof Map) {
            ((Map<String, Object>) instance).putAll(values);
            return;
        }
        values = Helpers.canonicalize(values);
        String fieldName = null;
        Map<String, List<BoundField>> fields = getBoundFields(instance
                .getClass());
        for (Entry<String, List<BoundField>> entry : fields.entrySet()) {
            fieldName = entry.getKey();
            if (values.containsKey(fieldName)) {
                entry.getValue().get(0)
                        .setValue(instance, values.get(fieldName));
            }
        }
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
     * 提取字段值转换为集合
     * 
     * @param instance
     * @return
     */
    public static Map<String, Object> toMap(Object instance) {
        Map<String, Object> values = new HashMap<String, Object>();
        Map<String, List<BoundField>> fields = getBoundFields(instance
                .getClass());
        for (Entry<String, List<BoundField>> entry : fields.entrySet()) {
            try {
                values.put(entry.getKey(),
                        entry.getValue().get(0).getValue(instance));
            } catch (Exception e) {
                LOG.warn("Failure getting field [{}] value.", entry.getKey(), e);
            }
        }
        return values;
    }

    public static <T> T toBean(Class<T> clazz, Object[] gridModel) {
        Object[] values = gridModel[1] instanceof Object[][] ? ((Object[][]) gridModel[1])[0]
                : (Object[]) gridModel[1];
        return toBean(clazz, (Object[]) gridModel[0], values);
    }

    public static <T> T toBean(Class<T> clazz, Object[] columns, Object[] values) {
        BoundField field;
        T bean = Reflects.newInstance(clazz);
        int size = Math.min(columns.length, values.length);
        for (int i = 0; i < size; i++) {
            field = getBoundField(clazz, String.valueOf(columns[i]));
            if (field != null) {
                field.setValue(bean, values[i]);
            }
        }
        return bean;
    }

    public static <T> List<T> toList(Class<T> clazz, Object[] gridModel) {
        List<T> result = new ArrayList<T>();
        String[] columns = (String[]) gridModel[0];
        for (Object[] values : (Object[][]) gridModel[1]) {
            result.add(toBean(clazz, columns, values));
        }
        return result;
    }

    public static Map<String, Object> toMap(Object[] gridModel) {
        Object[] values = gridModel[1] instanceof Object[][] ? ((Object[][]) gridModel[1])[0]
                : (Object[]) gridModel[1];
        return toMap((Object[]) gridModel[0], values);
    }

    public static Map<String, Object> toMap(Object[] columns, Object[] values) {
        Map<String, Object> bean = new HashMap<String, Object>();
        int size = Math.min(columns.length, values.length);
        for (int i = 0; i < size; i++) {
            bean.put(String.valueOf(columns[i]), values[i]);
        }
        return bean;
    }

    public static List<Map<String, Object>> toListMap(Object[] gridModel) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        String[] columns = (String[]) gridModel[0];
        for (Object[] values : (Object[][]) gridModel[1]) {
            result.add(toMap(columns, values));
        }
        return result;
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

    // private static final String SET_METHOD = "set";
    //
    // private static final int SET_METHOD_LEN = SET_METHOD.length();
    //
    // private static Map<Class<?>, Map<String, Method>> sms;
    //
    // static {
    // sms = new HashMap<Class<?>, Map<String, Method>>();
    // }
    //
    // private Reflects() {
    // }
    //
    // private static Map<String, Method> getSM(Class<?> clazz) {
    // return getMethods(clazz, SET_METHOD, SET_METHOD_LEN, null, sms);
    // }
    //
    // private static Map<String, Method> getMethods(Class<?> clazz,
    // String prefix, int prefixLen, String filter,
    // Map<Class<?>, Map<String, Method>> allMethods) {
    // Map<String, Method> result = allMethods.get(clazz);
    // if (result == null) {
    // result = new HashMap<String, Method>();
    // Method[] cms = clazz.getMethods();
    // String methodName = null;
    // for (Method method : cms) {
    // if ((methodName = method.getName()).startsWith(prefix)
    // && (filter == null || !methodName.equals(filter))) {
    // method.setAccessible(true);
    // result.put(makeKey(methodName, prefixLen), method);
    // }
    // }
    // allMethods.put(clazz, result);
    // }
    // return result;
    // }
    //
    // private static String makeKey(String methodName, int len) {
    // StringBuffer fieldName = new StringBuffer(32);
    // char[] chs = methodName.substring(len).toCharArray();
    // chs[0] = Character.toLowerCase(chs[0]);
    // for (char ch : chs) {
    // if (Character.isUpperCase(ch)) {
    // fieldName.append('_');
    // }
    // fieldName.append(Character.toLowerCase(ch));
    // }
    // return fieldName.toString();
    // }
    //
    // public static boolean isClass(String res) {
    // return getClass(res) != null;
    // }
    //
    // @SuppressWarnings("unchecked")
    // public static Map<String, Object> toMap(Object instance)
    // throws RuntimeException {
    // return (Map<String, Object>) Mapl.toMaplist(instance);
    // }
    //
    // @SuppressWarnings("unchecked")
    // public static <T> T toInstance(Class<T> clazz, Map<String, Object>
    // values)
    // throws RuntimeException {
    // return (T) Mapl.maplistToObj(values, clazz);
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
    // public static List<Map<String, Object>> toListMap(Object[] values) {
    // List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
    // if (values != null && values.length > 1) {
    // String[] columns = (String[]) values[0];
    // Object[][] rows = (Object[][]) values[1];
    // for (int i = 0; i < rows.length; i++) {
    // Map<String, Object> row = new HashMap<String, Object>();
    // for (int j = 0; j < columns.length; j++) {
    // row.put(columns[j], rows[i][j]);
    // }
    // result.add(row);
    // }
    // }
    // return result;
    // }
    //
    // /**
    // * 获取类型
    // *
    // * @param type
    // * @return
    // */
    // public static Class<?> getClass(String type) {
    // Class<?> result = null;
    // try {
    // result = Class.forName(type);
    // } catch (Exception e) {
    // }
    // return result;
    // }
    //
    // /**
    // * 获取实例
    // *
    // * @param type
    // * @return
    // */
    // public static Object getInstance(String type) {
    // Object result = null;
    // try {
    // result = getClass(type).newInstance();
    // } catch (Exception e) {
    // }
    // return result;
    // }
    //
    // /**
    // * 获取字段
    // *
    // * @param clazz
    // * @param fieldName
    // * @return
    // */
    // public static Field getField(Class<?> clazz, String fieldName) {
    // Field field = null;
    // try {
    // field = clazz.getDeclaredField(fieldName);
    // } catch (Exception e) {
    // }
    // if (field == null && clazz != null) {
    // field = getField(clazz.getSuperclass(), fieldName);
    // }
    // return field;
    // }
    //
    // /**
    // * 获取字段值
    // *
    // * @param object
    // * @param fieldName
    // * @return
    // */
    // public static Object getProperty(Object instance, String fieldName) {
    // Object result = null;
    // Field field = getField(instance.getClass(), fieldName);
    // if (field != null) {
    // field.setAccessible(true);
    // try {
    // result = field.get(instance);
    // } catch (Exception e) {
    // }
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
    //
    // private static void close(Closeable instance) {
    // if (instance != null) {
    // try {
    // instance.close();
    // } catch (IOException e) {
    // }
    // }
    // }
    //
    // public static Object getInstance(String type, Object... args) {
    // Object instance = null;
    // try {
    // instance = Mirror.me(Class.forName(type)).born(args);
    // } catch (ClassNotFoundException e) {
    // }
    // return instance;
    // }
    //
    // public static Object invoke(Object instance, String operate, Object...
    // args) {
    // return Mirror.me(instance.getClass()).invoke(instance, operate, args);
    // }
    //
    // public static Object cast(Map<String, Object> info, Type type) {
    // return Mapl.maplistToObj(info, type);
    // }

}

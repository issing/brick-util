package net.isger.util;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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

    public static final Object UNKNOWN = new Object();

    private static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

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
     * 获取类型名称
     *
     * @param type
     * @return
     */
    public static String getName(Type type) {
        return type instanceof Class ? ((Class<?>) type).getName()
                : type.toString();
    }

    public static GenericArrayType newArrayType(Type componentType) {
        return new GenericArrayTypeImpl(componentType);
    }

    public static ParameterizedType newParamType(Type ownerType, Type rawType,
            Type... arguments) {
        return new ParameterizedTypeImpl(ownerType, rawType, arguments);
    }

    public static WildcardType newUpperType(Type bound) {
        return new WildcardTypeImpl(new Type[] { bound }, EMPTY_TYPE_ARRAY);
    }

    public static WildcardType newLowerType(Type bound) {
        return new WildcardTypeImpl(new Type[] { Object.class },
                new Type[] { bound });
    }

    /**
     * 规范化
     *
     * @param type
     * @return
     */
    public static Type toCanonicalize(Type type) {
        if (type instanceof Class) {
            Class<?> pending = (Class<?>) type;
            return pending.isArray()
                    ? new GenericArrayTypeImpl(
                            toCanonicalize(pending.getComponentType()))
                    : pending;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pending = (ParameterizedType) type;
            return new ParameterizedTypeImpl(pending.getOwnerType(),
                    pending.getRawType(), pending.getActualTypeArguments());
        } else if (type instanceof GenericArrayType) {
            GenericArrayType pending = (GenericArrayType) type;
            return new GenericArrayTypeImpl(pending.getGenericComponentType());
        } else if (type instanceof WildcardType) {
            WildcardType pending = (WildcardType) type;
            return new WildcardTypeImpl(pending.getUpperBounds(),
                    pending.getLowerBounds());
        } else {
            return type;
        }
    }

    /**
     * 获取泛型落地类型
     *
     * @param contextType
     * @param rawClass
     * @param resolveType
     * @return
     */
    public static Type getResolveType(Type contextType, Class<?> rawClass,
            Type resolveType) {
        do {
            if (resolveType instanceof TypeVariable) {
                TypeVariable<?> typeVariable = (TypeVariable<?>) resolveType;
                resolveType = getResolveType(contextType, rawClass,
                        typeVariable);
                if (resolveType == typeVariable) {
                    return resolveType;
                }
            } else if (resolveType instanceof Class
                    && ((Class<?>) resolveType).isArray()) {
                Class<?> original = (Class<?>) resolveType;
                Type componentType = original.getComponentType();
                Type newComponentType = getResolveType(contextType, rawClass,
                        componentType);
                return componentType == newComponentType ? original
                        : newArrayType(newComponentType);
            } else if (resolveType instanceof GenericArrayType) {
                GenericArrayType original = (GenericArrayType) resolveType;
                Type componentType = original.getGenericComponentType();
                Type newComponentType = getResolveType(contextType, rawClass,
                        componentType);
                return componentType == newComponentType ? original
                        : newArrayType(newComponentType);
            } else if (resolveType instanceof ParameterizedType) {
                ParameterizedType original = (ParameterizedType) resolveType;
                Type ownerType = original.getOwnerType();
                Type newOwnerType = getResolveType(contextType, rawClass,
                        ownerType);
                boolean changed = newOwnerType != ownerType;
                Type[] args = original.getActualTypeArguments();
                for (int t = 0, length = args.length; t < length; t++) {
                    Type resolvedTypeArgument = getResolveType(contextType,
                            rawClass, args[t]);
                    if (resolvedTypeArgument != args[t]) {
                        if (!changed) {
                            args = args.clone();
                            changed = true;
                        }
                        args[t] = resolvedTypeArgument;
                    }
                }
                return changed ? newParamType(newOwnerType,
                        original.getRawType(), args) : original;
            } else if (resolveType instanceof WildcardType) {
                WildcardType original = (WildcardType) resolveType;
                Type[] originalLowerBound = original.getLowerBounds();
                Type[] originalUpperBound = original.getUpperBounds();
                if (originalLowerBound.length == 1) {
                    Type lowerBound = getResolveType(contextType, rawClass,
                            originalLowerBound[0]);
                    if (lowerBound != originalLowerBound[0]) {
                        return newLowerType(lowerBound);
                    }
                } else if (originalUpperBound.length == 1) {
                    Type upperBound = getResolveType(contextType, rawClass,
                            originalUpperBound[0]);
                    if (upperBound != originalUpperBound[0]) {
                        return newUpperType(upperBound);
                    }
                }
                return original;

            } else {
                return resolveType;
            }
        } while (true);
    }

    /**
     * 获取泛型落地类型
     *
     * @param contextType
     * @param rawClass
     * @param resolveType
     * @return
     */
    private static Type getResolveType(Type contextType, Class<?> rawClass,
            TypeVariable<?> resolveType) {
        GenericDeclaration declaring = resolveType.getGenericDeclaration();
        if (!(declaring instanceof Class)) {
            return resolveType;
        }
        contextType = Reflects.getSuperType(contextType, rawClass,
                (Class<?>) declaring);
        if (contextType instanceof ParameterizedType) {
            int index = Helpers.getIndex(declaring.getTypeParameters(),
                    resolveType);
            Asserts.throwArgument(index != -1, "No such %s", resolveType);
            return ((ParameterizedType) contextType)
                    .getActualTypeArguments()[index];
        }
        return resolveType;
    }

    /**
     * 获取泛型多态落地类型
     *
     * @param contextType
     * @param rawClass
     * @param resolveClass
     * @return
     */
    public static Type getSuperType(Type contextType, Class<?> rawClass,
            Class<?> resolveClass) {
        if (resolveClass == rawClass) {
            return contextType;
        }
        /* 目标为接口 */
        if (resolveClass.isInterface()) {
            Class<?>[] interfaces = rawClass.getInterfaces();
            for (int i = 0, length = interfaces.length; i < length; i++) {
                if (interfaces[i] == resolveClass) {
                    return rawClass.getGenericInterfaces()[i];
                } else if (resolveClass.isAssignableFrom(interfaces[i])) {
                    return getSuperType(rawClass.getGenericInterfaces()[i],
                            interfaces[i], resolveClass);
                }
            }
        }
        /* 目标为类 */
        if (!rawClass.isInterface()) {
            while (rawClass != Object.class) {
                Class<?> rawSuper = rawClass.getSuperclass();
                if (rawSuper == resolveClass) {
                    return rawClass.getGenericSuperclass();
                } else if (resolveClass.isAssignableFrom(rawSuper)) {
                    return getSuperType(rawClass.getGenericSuperclass(),
                            rawSuper, resolveClass);
                }
                rawClass = rawSuper;
            }
        }
        return resolveClass;
    }

    /**
     * 获取组件类型
     *
     * @param type
     * @return
     */
    public static Type getComponentType(Type type) {
        return type instanceof GenericArrayType
                ? ((GenericArrayType) type).getGenericComponentType()
                : ((Class<?>) type).getComponentType();
    }

    /**
     * 获取实际类型
     *
     * @param type
     * @return
     */
    public static Type getActualType(Type type) {
        return type instanceof ParameterizedType
                ? ((ParameterizedType) type).getActualTypeArguments()[0]
                : getRawClass(type);
    }

    /**
     * 获取元类型
     *
     * @param type
     * @return
     */
    public static Class<?> getRawClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            Asserts.throwArgument(rawType instanceof Class);
            return (Class<?>) rawType;
        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type)
                    .getGenericComponentType();
            return Array.newInstance(getRawClass(componentType), 0).getClass();
        } else if (type instanceof TypeVariable) {
            return Object.class;
        } else if (type instanceof WildcardType) {
            return getRawClass(((WildcardType) type).getUpperBounds()[0]);
        } else {
            String className = type == null ? "null"
                    : type.getClass().getName();
            throw Asserts.argument(
                    "Expected a Class, ParameterizedType, or GenericArrayType, but <%s> is of type %s",
                    type, className);
        }
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
        Class<?> pending = clazz;
        while (pending != null && pending != Object.class) {
            // 忽略指定类
            ignoreMode = getIgnoreMode(pending);
            // 导入声明字段（字段名优先）
            boundFields = new ArrayList<BoundField>();
            for (Field field : pending.getDeclaredFields()) {
                if ((boundField = createBoundField(field, ignoreMode)) != null
                        && Helpers.toAppend(result, boundField.getName(),
                                boundField)) {
                    boundFields.add(boundField);
                }
            }
            // 导入别名字段（候选）
            for (BoundField field : boundFields) {
                name = field.getAlias();
                if (name != null) {
                    Helpers.toAppend(result, name, field);
                }
            }
            pending = pending.getSuperclass();
        }
        // 屏蔽修改
        FIELDS.put(clazz, result = Helpers.toUnmodifiable(result));
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
        if (Mode.EXCLUDE.equals(getIgnoreMode(ignore, mode))) {
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
                        && Helpers.toAppend(result, boundMethod.getName(),
                                boundMethod)
                        && Helpers.toAppend(result, boundMethod.getMethodDesc(),
                                boundMethod)) {
                    boundMethods.add(boundMethod);
                }
            }
            // 导入别名及描述名方法（候选）
            for (BoundMethod method : boundMethods) {
                name = method.getAliasName();
                if (name != null) {
                    Helpers.toAppend(result, name, method);
                }
                Helpers.toAppend(result, method.getMethodDesc(), method);
            }
            type = type.getSuperclass();
        }
        // 屏蔽修改
        METHODS.put(clazz, result = Helpers.toUnmodifiable(result));
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
        if (Mode.EXCLUDE.equals(getIgnoreMode(ignore, mode))) {
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
        Mode mode = getIgnoreMode(ignore, null);
        if (mode != null) {
            return mode;
        }
        /* 忽略配置文件（TODO 忽略配置规则不完善） */
        String path = clazz.getName().replaceAll("[.]", "/");
        String name = clazz.getSimpleName();
        Properties props = new Properties();
        Helpers.load(props, path.substring(0, path.length() - name.length())
                + ".ignoreMode");
        Helpers.load(props, path + ".ignoreMode");
        String modeName = props.getProperty("this");
        if (Mode.EXCLUDE_NAME.equals(modeName)) {
            mode = Mode.EXCLUDE;
        } else {
            mode = Mode.INCLUDE;
        }
        return mode;
    }

    private static Mode getIgnoreMode(Ignore ignore, Mode mode) {
        if (ignore != null) {
            Mode result = ignore.mode();
            if (result != null) {
                return result;
            }
        }
        return mode;
    }

    /**
     * 集合填充生成指定类型对象
     * 
     * @param name
     * @param params
     * @return
     */
    public static Object newInstance(Map<String, Object> params) {
        return newInstance(params, (Callable<?>) null);
    }

    /**
     * 集合填充生成指定类型对象
     * 
     *
     * @param params
     * @param assembler
     * @return
     */
    public static Object newInstance(Map<String, Object> params,
            Callable<?> assembler) {
        Object type = params.get(KEY_CLASS);
        if (type == null) {
            return params;
        }
        params.remove(KEY_CLASS);
        Class<?> clazz = type instanceof Class ? (Class<?>) type
                : getClass((String) type);
        Asserts.isNotNull(clazz, "Cannot instantiation type " + type);
        return newInstance(clazz, params, assembler);
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
     * @param params
     * @return
     */
    public static <T> T newInstance(Class<T> clazz,
            Map<String, Object> params) {
        return newInstance(clazz, params, (Callable<?>) null);
    }

    /**
     * 集合填充生成指定类型对象
     *
     * @param clazz
     * @param params
     * @param assembler
     * @return
     */
    public static <T> T newInstance(Class<T> clazz, Map<String, Object> params,
            Callable<?> assembler) {
        T instance = newInstance(clazz);
        if (params != null && params.size() > 0) {
            toInstance(instance, params, assembler);
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
     * @param rawClass
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<? extends T> rawClass) {
        if (isAbstract(rawClass)) {
            if (Collection.class.isAssignableFrom(rawClass)) {
                if (SortedSet.class.isAssignableFrom(rawClass)) {
                    return (T) new TreeSet<Object>();
                } else if (Set.class.isAssignableFrom(rawClass)) {
                    return (T) new LinkedHashSet<Object>();
                } else if (Queue.class.isAssignableFrom(rawClass)) {
                    return (T) new LinkedList<Object>();
                } else {
                    return (T) new ArrayList<Object>();
                }
            }
            // return new Standin(clazz).getSource();
            throw Asserts.state("Unsupport %s", rawClass);
        }
        return Constructor.construct(rawClass);
    }

    /**
     * 设置实例所有属性值（通过集合数据）
     * 
     * @param type
     * @param values
     * @return
     */
    public static <T> T toInstance(T instance, Map<String, Object> params) {
        return toInstance(instance, params, null);
    }

    /**
     * 设置实例所有属性值（通过集合数据）
     * 
     *
     * @param instance
     * @param params
     * @param assembler
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T toInstance(T instance, Map<String, Object> params,
            final Callable<?> assembler) {
        if (instance instanceof Map) {
            ((Map<String, Object>) instance).putAll(params);
            return instance;
        }
        final Map<String, Object> values = Helpers.toHierarchical(params);
        String fieldName;
        BoundField field;
        Map<String, List<BoundField>> fields = getBoundFields(
                instance.getClass());
        Callable<?> fieldAssembler = assembler == null
                ? new Callable<Object>() {
                    public Object call(Object... args) {
                        return args[2]; // 直接返回字段实例
                    }
                } : new Callable<Object>() {
                    public Object call(Object... args) {
                        return assembler.call(
                                (Object[]) Helpers.newArray(args, values));
                    }
                };
        for (Entry<String, List<BoundField>> entry : fields.entrySet()) {
            fieldName = entry.getKey();
            field = entry.getValue().get(0);
            if (values.containsKey(fieldName)) {
                field.setValue(instance, values.get(fieldName), fieldAssembler);
            } else {
                field.setValue(instance, UNKNOWN, fieldAssembler);
            }
        }
        return instance;
    }

    /**
     * 设置实例属性值
     * 
     * @param instance
     * @param name
     * @param value
     */
    public static void toField(Object instance, String name, Object value) {
        Class<?> rawClass = instance.getClass();
        BoundField field = getBoundField(rawClass, name);
        if (field == null) {
            throw Asserts.state("Not found field in %s by %s", rawClass, name);
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
        return toBean(clazz, columns, values, null);
    }

    /**
     * 根据网格模型转换为实例
     * 
     *
     * @param clazz
     * @param columns
     * @param values
     * @param assembler
     * @return
     */
    public static <T> T toBean(Class<T> clazz, Object[] columns,
            Object[] values, Callable<?> assembler) {
        return Reflects.newInstance(clazz, toMap(columns, values), assembler);
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
        Map<String, Object> result = new HashMap<String, Object>(size);
        for (int i = 0; i < size; i++) {
            result.put(String.valueOf(columns[i]), values[i]);
        }
        return result;
    }

    /**
     * 根据网格模型转换为集合
     * 
     * @param clazz
     * @param grid
     * @return
     */
    public static <T> List<T> toList(Class<T> clazz, Object[] grid) {
        return toList(clazz, grid, null);
    }

    /**
     * 根据网格模型转换为集合
     *
     * @param clazz
     * @param grid
     * @param assembler
     * @return
     */
    public static <T> List<T> toList(Class<T> clazz, Object[] grid,
            Callable<?> assembler) {
        List<T> result = new ArrayList<T>();
        Object[] columns = (Object[]) grid[0];
        Object gridValue = grid[1];
        if (gridValue instanceof Object[][]) {
            for (Object[] values : (Object[][]) gridValue) {
                result.add(toBean(clazz, columns, values, assembler));
            }
        } else if (gridValue instanceof Object[]) {
            result.add(toBean(clazz, columns, (Object[]) gridValue, assembler));
        }
        return result;
    }

    /**
     * 根据键值对集合转换为目标实例集合
     *
     * @param clazz
     * @param values
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> toList(Class<T> clazz,
            List<Map<String, Object>> values) {
        return toList(clazz, values, new Callable<T>() {
            public T call(Object... args) {
                return (T) args[1];
            }
        });
    }

    /**
     * 根据键值对集合转换为目标实例集合
     *
     * @param clazz
     * @param values
     * @param interceptor
     * @return
     */
    public static <T> List<T> toList(Class<T> clazz,
            List<Map<String, Object>> values, Callable<T> interceptor) {
        List<T> result = new ArrayList<T>(values.size());
        int step = 0;
        T instance;
        for (Map<String, Object> value : values) {
            instance = interceptor.call(step++, newInstance(clazz, value),
                    result);
            if (instance != null) {
                result.add(instance);
            }
        }
        return result;
    }

    /**
     * 根据网格模型转换为集合
     *
     * @param grid
     * @return
     */
    public static List<Map<String, Object>> toList(Object[] grid) {
        return toList((Object[]) grid[0], (Object[][]) grid[1]);
    }

    /**
     * 根据网格模型转换为集合
     *
     * @param columns
     * @param values
     * @return
     */
    public static List<Map<String, Object>> toList(Object[] columns,
            Object[][] values) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(
                values.length);
        for (Object[] value : values) {
            result.add(toMap(columns, value));
        }
        return result;
    }

    /**
     * 类型是否相同
     *
     * @param source
     * @param target
     * @return
     */
    public static boolean equals(Type source, Type target) {
        if (source == target) {
            return true;
        } else if (source instanceof Class) {
            return source.equals(target);
        } else if (source instanceof ParameterizedType) {
            if (!(target instanceof ParameterizedType)) {
                return false;
            }
            ParameterizedType pa = (ParameterizedType) source;
            ParameterizedType pb = (ParameterizedType) target;
            return Helpers.equals(pa.getOwnerType(), pb.getOwnerType())
                    && pa.getRawType().equals(pb.getRawType())
                    && Arrays.equals(pa.getActualTypeArguments(),
                            pb.getActualTypeArguments());
        } else if (source instanceof GenericArrayType) {
            if (!(target instanceof GenericArrayType)) {
                return false;
            }
            GenericArrayType ga = (GenericArrayType) source;
            GenericArrayType gb = (GenericArrayType) target;
            return equals(ga.getGenericComponentType(),
                    gb.getGenericComponentType());
        } else if (source instanceof WildcardType) {
            if (!(target instanceof WildcardType)) {
                return false;
            }
            WildcardType wa = (WildcardType) source;
            WildcardType wb = (WildcardType) target;
            return Arrays.equals(wa.getUpperBounds(), wb.getUpperBounds())
                    && Arrays.equals(wa.getLowerBounds(), wb.getLowerBounds());

        } else if (source instanceof TypeVariable) {
            if (!(target instanceof TypeVariable)) {
                return false;
            }
            TypeVariable<?> va = (TypeVariable<?>) source;
            TypeVariable<?> vb = (TypeVariable<?>) target;
            return va.getGenericDeclaration() == vb.getGenericDeclaration()
                    && va.getName().equals(vb.getName());
        } else {
            return false;
        }
    }

    /**
     * 参数泛型
     * 
     * @author issing
     */
    private static final class ParameterizedTypeImpl
            implements ParameterizedType, Serializable {
        private static final long serialVersionUID = 5081438518083630676L;

        private final Type ownerType;

        private final Type rawType;

        private final Type[] typeArguments;

        public ParameterizedTypeImpl(Type ownerType, Type rawType,
                Type... typeArguments) {
            if (rawType instanceof Class<?>) {
                Class<?> rawClass = (Class<?>) rawType;
                Asserts.throwArgument(ownerType != null
                        || rawClass.getEnclosingClass() == null);
                Asserts.throwArgument(ownerType == null
                        || rawClass.getEnclosingClass() != null);
            }
            this.ownerType = ownerType == null ? null
                    : Reflects.toCanonicalize(ownerType);
            this.rawType = Reflects.toCanonicalize(rawType);
            this.typeArguments = typeArguments.clone();
            for (int i = 0; i < this.typeArguments.length; i++) {
                Asserts.isNotNull(this.typeArguments[i]);
                Asserts.isNotPrimitive(this.typeArguments[i]);
                this.typeArguments[i] = toCanonicalize(this.typeArguments[i]);
            }
        }

        public Type[] getActualTypeArguments() {
            return typeArguments.clone();
        }

        public Type getRawType() {
            return rawType;
        }

        public Type getOwnerType() {
            return ownerType;
        }

        public boolean equals(Object other) {
            return other instanceof ParameterizedType
                    && Reflects.equals(this, (ParameterizedType) other);
        }

        public int hashCode() {
            return Arrays.hashCode(typeArguments) ^ rawType.hashCode()
                    ^ Helpers.hashCode(ownerType);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(
                    30 * (typeArguments.length + 1));
            stringBuilder.append(getName(rawType));
            if (typeArguments.length == 0) {
                return stringBuilder.toString();
            }
            stringBuilder.append("<").append(getName(typeArguments[0]));
            for (int i = 1; i < typeArguments.length; i++) {
                stringBuilder.append(", ").append(getName(typeArguments[i]));
            }
            return stringBuilder.append(">").toString();
        }
    }

    /**
     * 数组泛型
     * 
     * @author issing
     */
    private static final class GenericArrayTypeImpl
            implements GenericArrayType, Serializable {
        private static final long serialVersionUID = -1183771465139410856L;

        private final Type componentType;

        public GenericArrayTypeImpl(Type componentType) {
            this.componentType = toCanonicalize(componentType);
        }

        public Type getGenericComponentType() {
            return componentType;
        }

        public boolean equals(Object o) {
            return o instanceof GenericArrayType
                    && Reflects.equals(this, (GenericArrayType) o);
        }

        public int hashCode() {
            return componentType.hashCode();
        }

        public String toString() {
            return getName(componentType) + "[]";
        }
    }

    /**
     * 通配符泛型
     * 
     * @author issing
     */
    private static final class WildcardTypeImpl
            implements WildcardType, Serializable {
        private static final long serialVersionUID = 8303914422137884485L;

        private final Type upperBound;

        private final Type lowerBound;

        public WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
            Asserts.throwArgument(lowerBounds.length <= 1);
            Asserts.throwArgument(upperBounds.length == 1);
            if (lowerBounds.length == 1) {
                Asserts.isNotNull(lowerBounds[0]);
                Asserts.isNotPrimitive(lowerBounds[0]);
                Asserts.throwArgument(upperBounds[0] == Object.class);
                this.lowerBound = toCanonicalize(lowerBounds[0]);
                this.upperBound = Object.class;
            } else {
                Asserts.isNotNull(upperBounds[0]);
                Asserts.isNotPrimitive(upperBounds[0]);
                this.lowerBound = null;
                this.upperBound = toCanonicalize(upperBounds[0]);
            }
        }

        public Type[] getUpperBounds() {
            return new Type[] { upperBound };
        }

        public Type[] getLowerBounds() {
            return lowerBound != null ? new Type[] { lowerBound }
                    : EMPTY_TYPE_ARRAY;
        }

        public boolean equals(Object other) {
            return other instanceof WildcardType
                    && Reflects.equals(this, (WildcardType) other);
        }

        public int hashCode() {
            return (lowerBound != null ? 31 + lowerBound.hashCode() : 1)
                    ^ (31 + upperBound.hashCode());
        }

        public String toString() {
            if (lowerBound != null) {
                return "? super " + getName(lowerBound);
            } else if (upperBound == Object.class) {
                return "?";
            } else {
                return "? extends " + getName(upperBound);
            }
        }
    }
}

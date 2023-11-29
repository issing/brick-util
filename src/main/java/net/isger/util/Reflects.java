package net.isger.util;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
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
import java.util.Date;
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
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.isger.util.anno.Ignore;
import net.isger.util.anno.Ignore.Mode;
import net.isger.util.reflect.AssemblerAdapter;
import net.isger.util.reflect.BoundField;
import net.isger.util.reflect.BoundMethod;
import net.isger.util.reflect.ClassAssembler;
import net.isger.util.reflect.Constructor;
import net.isger.util.reflect.Converter;

/**
 * 反射工具
 * 
 * @author issing
 */
public class Reflects {

    public static final Object UNKNOWN = new Object();

    private static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

    /** 反射类配置键 */
    public static final String KEY_CLASS = "class";

    /** 包装类型集合 */
    private static final Map<Class<?>, Class<?>> WRAP_TYPES;

    /** 原始类型集合 */
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPES;

    private static final Logger LOG;

    /** 类字段缓存 */
    private static final Map<Class<?>, Map<String, List<BoundField>>> FIELDS;

    /** 类方法缓存 */
    private static final Map<Class<?>, Map<String, List<BoundMethod>>[]> METHODS;

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

        PRIMITIVE_TYPES = new HashMap<Class<?>, Class<?>>();
        PRIMITIVE_TYPES.put(Void.class, Void.TYPE);
        PRIMITIVE_TYPES.put(Boolean.class, Boolean.TYPE);
        PRIMITIVE_TYPES.put(Byte.class, Byte.TYPE);
        PRIMITIVE_TYPES.put(Character.class, Character.TYPE);
        PRIMITIVE_TYPES.put(Short.class, Short.TYPE);
        PRIMITIVE_TYPES.put(Integer.class, Integer.TYPE);
        PRIMITIVE_TYPES.put(Long.class, Long.TYPE);
        PRIMITIVE_TYPES.put(Float.class, Float.TYPE);
        PRIMITIVE_TYPES.put(Double.class, Double.TYPE);

        LOG = LoggerFactory.getLogger(Reflects.class);

        FIELDS = new ConcurrentHashMap<Class<?>, Map<String, List<BoundField>>>();
        METHODS = new ConcurrentHashMap<Class<?>, Map<String, List<BoundMethod>>[]>();
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
        return type instanceof Class ? ((Class<?>) type).getName() : type.toString();
    }

    public static GenericArrayType newArrayType(Type componentType) {
        return new GenericArrayTypeImpl(componentType);
    }

    public static ParameterizedType newParamType(Type ownerType, Type rawType, Type... arguments) {
        return new ParameterizedTypeImpl(ownerType, rawType, arguments);
    }

    public static WildcardType newUpperType(Type boundType) {
        return new WildcardTypeImpl(new Type[] { boundType }, EMPTY_TYPE_ARRAY);
    }

    public static WildcardType newLowerType(Type boundType) {
        return new WildcardTypeImpl(new Type[] { Object.class }, new Type[] { boundType });
    }

    /**
     * 获取注解
     * 
     * @param instance
     * @param clazz
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAnnotation(Object instance, Class<T> clazz) {
        Annotation[] annos;
        if (instance instanceof Class) {
            annos = ((Class<?>) instance).getDeclaredAnnotations();
        } else if (instance instanceof Method) {
            annos = ((Method) instance).getDeclaredAnnotations();
        } else if (instance instanceof AnnotatedElement) {
            annos = ((AnnotatedElement) instance).getAnnotations();
        } else if (instance != null) {
            annos = instance.getClass().getDeclaredAnnotations();
        } else {
            return null;
        }
        for (Annotation anno : annos) {
            if (clazz.isInstance(anno)) {
                return (T) anno;
            }
        }
        return null;
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
            return pending.isArray() ? new GenericArrayTypeImpl(toCanonicalize(pending.getComponentType())) : pending;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pending = (ParameterizedType) type;
            return new ParameterizedTypeImpl(pending.getOwnerType(), pending.getRawType(), pending.getActualTypeArguments());
        } else if (type instanceof GenericArrayType) {
            GenericArrayType pending = (GenericArrayType) type;
            return new GenericArrayTypeImpl(pending.getGenericComponentType());
        } else if (type instanceof WildcardType) {
            WildcardType pending = (WildcardType) type;
            return new WildcardTypeImpl(pending.getUpperBounds(), pending.getLowerBounds());
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
    public static Type getResolveType(Type contextType, Class<?> rawClass, Type resolveType) {
        do {
            if (resolveType instanceof TypeVariable) {
                TypeVariable<?> typeVariable = (TypeVariable<?>) resolveType;
                resolveType = getResolveType(contextType, rawClass, typeVariable);
                if (resolveType == typeVariable) {
                    return resolveType;
                }
            } else if (resolveType instanceof Class && ((Class<?>) resolveType).isArray()) {
                Class<?> original = (Class<?>) resolveType;
                Type componentType = original.getComponentType();
                Type newComponentType = getResolveType(contextType, rawClass, componentType);
                return componentType == newComponentType ? original : newArrayType(newComponentType);
            } else if (resolveType instanceof GenericArrayType) {
                GenericArrayType original = (GenericArrayType) resolveType;
                Type componentType = original.getGenericComponentType();
                Type newComponentType = getResolveType(contextType, rawClass, componentType);
                return componentType == newComponentType ? original : newArrayType(newComponentType);
            } else if (resolveType instanceof ParameterizedType) {
                ParameterizedType original = (ParameterizedType) resolveType;
                Type ownerType = original.getOwnerType();
                Type newOwnerType = getResolveType(contextType, rawClass, ownerType);
                boolean changed = newOwnerType != ownerType;
                Type[] args = original.getActualTypeArguments();
                for (int t = 0, length = args.length; t < length; t++) {
                    Type resolvedTypeArgument = getResolveType(contextType, rawClass, args[t]);
                    if (resolvedTypeArgument != args[t]) {
                        if (!changed) {
                            args = args.clone();
                            changed = true;
                        }
                        args[t] = resolvedTypeArgument;
                    }
                }
                return changed ? newParamType(newOwnerType, original.getRawType(), args) : original;
            } else if (resolveType instanceof WildcardType) {
                WildcardType original = (WildcardType) resolveType;
                Type[] originalLowerBound = original.getLowerBounds();
                Type[] originalUpperBound = original.getUpperBounds();
                if (originalLowerBound.length == 1) {
                    Type lowerBound = getResolveType(contextType, rawClass, originalLowerBound[0]);
                    if (lowerBound != originalLowerBound[0]) {
                        return newLowerType(lowerBound);
                    }
                } else if (originalUpperBound.length == 1) {
                    Type upperBound = getResolveType(contextType, rawClass, originalUpperBound[0]);
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
    private static Type getResolveType(Type contextType, Class<?> rawClass, TypeVariable<?> resolveType) {
        GenericDeclaration declaring = resolveType.getGenericDeclaration();
        if (!(declaring instanceof Class)) {
            return resolveType;
        }
        contextType = Reflects.getSuperType(contextType, rawClass, (Class<?>) declaring);
        if (contextType instanceof ParameterizedType) {
            int index = Helpers.getIndex(declaring.getTypeParameters(), resolveType);
            Asserts.throwArgument(index != -1, "No such %s", resolveType);
            return ((ParameterizedType) contextType).getActualTypeArguments()[index];
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
    public static Type getSuperType(Type contextType, Class<?> rawClass, Class<?> resolveClass) {
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
                    return getSuperType(rawClass.getGenericInterfaces()[i], interfaces[i], resolveClass);
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
                    return getSuperType(rawClass.getGenericSuperclass(), rawSuper, resolveClass);
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
        return type instanceof GenericArrayType ? ((GenericArrayType) type).getGenericComponentType() : ((Class<?>) type).getComponentType();
    }

    /**
     * 获取实际类型
     *
     * @param type
     * @return
     */
    public static Type getActualType(Type type) {
        return type instanceof ParameterizedType ? ((ParameterizedType) type).getActualTypeArguments()[0] : getRawClass(type);
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
            Asserts.isInstance(Class.class, rawType);
            return (Class<?>) rawType;
        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawClass(componentType), 0).getClass();
        } else if (type instanceof TypeVariable) {
            return Object.class;
        } else if (type instanceof WildcardType) {
            return getRawClass(((WildcardType) type).getUpperBounds()[0]);
        } else {
            String className = type == null ? "null" : type.getClass().getName();
            throw Asserts.argument("Expected a Class, ParameterizedType, or GenericArrayType, but [%s] is of class [%s]", type, className);
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
     * @param rawClass
     * @return
     */
    public static boolean isAbstract(Class<?> rawClass) {
        return Modifier.isAbstract(rawClass.getModifiers());
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
     * 是否为常规类
     *
     * @param instance
     * @return
     */
    public static boolean isGeneral(Class<?> rawClass) {
        boolean general = getPrimitiveClass(rawClass) != null || Object.class == rawClass;
        if (!(general || rawClass == null)) {
            general = CharSequence.class.isAssignableFrom(rawClass) || Date.class.isAssignableFrom(rawClass) || Map.class.isAssignableFrom(rawClass) || Collection.class.isAssignableFrom(rawClass) || rawClass.isArray();
        }
        return general;
    }

    /**
     * 获取类型
     * 
     * @param instance
     * @return
     */
    public static Class<?> getClass(Object instance) {
        Class<?> raw = null;
        if (instance instanceof String) {
            raw = getClass((String) instance);
        } else if (instance instanceof Class) {
            raw = (Class<?>) instance;
        } else if (instance != null) {
            raw = instance.getClass();
        }
        return raw;
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
     * @param loader
     * @return
     */
    public static Class<?> getClass(String name, ClassLoader loader) {
        if (loader == null) {
            loader = getClassLoader();
        }
        Class<?> rawClass = null;
        try {
            String[] form = name.split("[ ]", 2);
            name = form.length > 1 ? form[1] : form[0];
            rawClass = loader != null ? loader.loadClass(name) : Class.forName(name);
        } catch (Exception ex) {
        }
        return rawClass;
    }

    /**
     * 获取包装类型
     * 
     * @param primitiveClass
     * @return
     */
    public static Class<?> getWrapClass(Class<?> primitiveClass) {
        Class<?> wrap = WRAP_TYPES.get(primitiveClass);
        if (wrap == null) {
            wrap = primitiveClass;
        }
        return wrap;
    }

    /**
     * 获取原始类型
     *
     * @param wrapClass
     * @return
     */
    public static Class<?> getPrimitiveClass(Class<?> wrapClass) {
        if (wrapClass.isPrimitive()) {
            return wrapClass;
        }
        return PRIMITIVE_TYPES.get(wrapClass);
    }

    /**
     * 获取所有接口
     *
     * @param rawClass
     * @return
     */
    public static Class<?>[] getInterfaces(Class<?> rawClass) {
        List<Class<?>> interfaces = new ArrayList<Class<?>>();
        appendInterfaces(interfaces, rawClass);
        return interfaces.toArray(new Class<?>[interfaces.size()]);
    }

    /**
     * 递归追加接口
     *
     * @param container
     * @param rawClass
     */
    private static void appendInterfaces(List<Class<?>> container, Class<?> rawClass) {
        if (!(rawClass == null || container.contains(rawClass))) {
            if (rawClass.isInterface()) {
                container.add(rawClass);
            }
            for (Class<?> interfaceClass : rawClass.getInterfaces()) {
                if (container.contains(interfaceClass)) {
                    continue;
                }
                appendInterfaces(container, interfaceClass);
            }
            appendInterfaces(container, rawClass.getSuperclass());
        }
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
        } catch (Throwable e) {
        }
        classLoader = Reflects.class.getClassLoader();
        if (classLoader == null) {
            try {
                classLoader = ClassLoader.getSystemClassLoader();
            } catch (Throwable e) {
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
        Enumeration<URL> pending;
        List<URL> resources = new ArrayList<URL>();
        ClassLoader loader = Reflects.getClassLoader(source);
        get: try {
            pending = loader.getResources(name);
            if (pending == null) {
                if (loader == ClassLoader.getSystemClassLoader()) {
                    break get;
                }
                pending = ClassLoader.getSystemClassLoader().getResources(name);
                if (pending == null) {
                    break get;
                }
            }
            while (pending.hasMoreElements()) {
                resources.add(pending.nextElement());
            }
        } catch (Exception e) {
        }
        return resources;
    }

    public static URL getResource(String name) {
        return getResource(null, name);
    }

    public static URL getResource(Object source, String name) {
        ClassLoader loader = Reflects.getClassLoader(source);
        URL resource;
        get: try {
            resource = loader.getResource(name);
            if (resource == null) {
                if (loader == ClassLoader.getSystemClassLoader()) {
                    break get;
                }
                resource = ClassLoader.getSystemClassLoader().getResource(name);
            }
        } catch (Exception e) {
            resource = null;
        }
        return resource;
    }

    /**
     * 获取绑定字段信息
     * 
     * @param rawClass
     * @return
     */
    public static Map<String, List<BoundField>> getBoundFields(Class<?> rawClass) {
        Map<String, List<BoundField>> result = FIELDS.get(rawClass);
        // 跳过接口以及原始数据类型（不继承Object类）
        if (result != null || rawClass.isInterface() || !Object.class.isAssignableFrom(rawClass)) {
            return result;
        }
        result = new LinkedHashMap<String, List<BoundField>>();
        Mode ignoreMode;
        BoundField boundField;
        // List<BoundField> boundFields;
        Class<?> pending = rawClass;
        while (pending != null && pending != Object.class) {
            // 忽略指定类
            ignoreMode = getIgnoreMode(pending);
            // 导入声明字段（字段名优先）
            // boundFields = new ArrayList<BoundField>();
            for (Field field : pending.getDeclaredFields()) {
                if ((boundField = createBoundField(field, ignoreMode)) != null) {
                    // boundFields.add(boundField);
                    Helpers.toAppend(result, boundField.getName(), boundField);
                }
            }
            pending = pending.getSuperclass();
        }
        // 屏蔽修改
        FIELDS.put(rawClass, result = Helpers.toUnmodifiable(result));
        return result;
    }

    /**
     * 获取绑定字段信息
     * 
     * @param rawClass
     * @param fieldName
     * @return
     */
    public static BoundField getBoundField(Class<?> rawClass, String fieldName) {
        Map<String, List<BoundField>> boundFields = getBoundFields(rawClass);
        List<BoundField> bounds = boundFields.get(fieldName);
        if (bounds != null && bounds.size() > 0) {
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
        if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || Modifier.isTransient(mod) || Modifier.isVolatile(mod)) {
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
     * @param rawClass
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map<String, List<BoundMethod>> getBoundMethods(Class<?> rawClass, boolean overall) {
        Map<String, List<BoundMethod>>[] result = METHODS.get(rawClass);
        // 跳过接口以及原始数据类型（不继承Object类）
        if (result != null || !Object.class.isAssignableFrom(rawClass)) {
            return result[overall ? 1 : 0];
        }
        result = new Map[] { new LinkedHashMap<String, List<BoundMethod>>(), new LinkedHashMap<String, List<BoundMethod>>() };
        String name;
        Mode ignoreMode;
        BoundMethod boundMethod;
        Class<?> type = rawClass;
        while (type != null && type != Object.class) {
            // 忽略指定类
            ignoreMode = getIgnoreMode(type);
            // 导入声明方法（方法名优先）
            for (Method method : type.getDeclaredMethods()) {
                if ((boundMethod = createBoundMethod(method, ignoreMode)) != null && Helpers.toAppend(result[0], name = method.getName(), boundMethod)) {
                    // 导入别名及描述名方法（候选）
                    Helpers.toAppend(result[1], name, boundMethod);
                    if (Strings.isNotEmpty(name = boundMethod.getAliasName())) {
                        Helpers.toAppend(result[1], name, boundMethod);
                    }
                    Helpers.toAppend(result[1], boundMethod.getMethodDesc(), boundMethod);
                }
            }
            type = type.getSuperclass();
        }
        // 屏蔽修改
        METHODS.put(rawClass, result = new Map[] { Helpers.toUnmodifiable(result[0]), Helpers.toUnmodifiable(result[1]) });
        return result[overall ? 1 : 0];
    }

    /**
     * 获取绑定方法信息
     * 
     * @param rawClass
     * @param name
     * @return
     */
    public static BoundMethod getBoundMethod(Class<?> rawClass, String name, boolean overall) {
        List<BoundMethod> bounds = getBoundMethods(rawClass, overall).get(name);
        return bounds == null || bounds.size() == 0 ? null : bounds.get(0);
    }

    /**
     * 获取绑定方法信息
     *
     * @param <T>
     * @param rawClass
     * @param annoClass
     * @return
     */
    public static <T extends Annotation> List<BoundMethod> getBoundMethods(Class<?> rawClass, Class<T> annoClass, boolean overall) {
        List<BoundMethod> result = new ArrayList<BoundMethod>();
        for (List<BoundMethod> bounds : getBoundMethods(rawClass, overall).values()) {
            result.addAll(getBoundMethods(bounds, annoClass));
        }
        return result;
    }

    /**
     * 获取绑定方法信息
     *
     * @param <T>
     * @param bounds
     * @param annoClass
     * @return
     */
    private static <T extends Annotation> List<BoundMethod> getBoundMethods(List<BoundMethod> bounds, Class<T> annoClass) {
        List<BoundMethod> methods = new ArrayList<BoundMethod>();
        for (BoundMethod bound : bounds) {
            if (bound.getAnnotation(annoClass) != null) {
                methods.add(bound);
            }
        }
        return methods;
    }

    /**
     * 获取绑定方法信息
     *
     * @param <T>
     * @param rawClass
     * @param annoClass
     * @return
     */
    public static <T extends Annotation> BoundMethod getBoundMethod(Class<?> rawClass, Class<T> annoClass, boolean overall) {
        BoundMethod method = null;
        for (List<BoundMethod> bounds : getBoundMethods(rawClass, overall).values()) {
            if ((method = getBoundMethod(bounds, annoClass)) != null) {
                break;
            }
        }
        return method;
    }

    /**
     * 获取绑定方法信息
     *
     * @param <T>
     * @param rawClass
     * @param name
     * @param annoClass
     * @return
     */
    public static <T extends Annotation> BoundMethod getBoundMethod(Class<?> rawClass, String name, Class<T> annoClass, boolean overall) {
        List<BoundMethod> bounds = getBoundMethods(rawClass, overall).get(name);
        if (bounds != null) {
            return getBoundMethod(bounds, annoClass);
        }
        return null;
    }

    /**
     * 获取绑定方法信息
     *
     * @param <T>
     * @param bounds
     * @param anno
     * @return
     */
    private static <T extends Annotation> BoundMethod getBoundMethod(List<BoundMethod> bounds, Class<T> anno) {
        for (BoundMethod bound : bounds) {
            if (bound.getAnnotation(anno) != null) {
                return bound;
            }
        }
        return null;
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
     * @param rawClass
     * @return
     */
    private static Mode getIgnoreMode(Class<?> rawClass) {
        Ignore ignore = rawClass.getAnnotation(Ignore.class);
        Mode mode = getIgnoreMode(ignore, null);
        if (mode != null) {
            return mode;
        }
        /* 忽略配置文件（TODO 忽略配置规则不完善） */
        String name = rawClass.getSimpleName();
        String path = rawClass.getName().replaceAll("[.]", "/");
        path = path.substring(0, path.length() - name.length());
        Properties props = new Properties();
        String p = "";
        if (path.length() > 0) {
            for (String n : path.split("[/]")) {
                Helpers.load(props, false, (p += n + "/") + ".ignoreMode");
            }
        }
        Helpers.load(props, false, p + name + ".ignoreMode");
        String modeName = props.getProperty("this");
        if (Mode.EXCLUDE_NAME.equals(modeName)) {
            mode = Mode.EXCLUDE;
        } else {
            mode = Mode.INCLUDE;
        }
        return mode;
    }

    /**
     * 获取忽略模式
     *
     * @param ignore
     * @param mode
     * @return
     */
    private static Mode getIgnoreMode(Ignore ignore, Mode mode) {
        Mode result = mode;
        if (ignore != null) {
            if ((result = ignore.mode()) == null) {
                result = mode;
            }
        }
        return result;
    }

    /**
     * 集合填充生成指定类型对象
     * 
     * @param params
     * @param namespace
     * @return
     */
    public static Object newInstance(Map<String, ? extends Object> params, String namespace) {
        return newInstance(Helpers.getMap(params, namespace));
    }

    /**
     * 集合填充生成指定类型对象
     * 
     * @param rawClass
     * @param params
     * @param namespace
     * @return
     */
    public static <T> T newInstance(Class<T> rawClass, Map<String, ? extends Object> params, String namespace) {
        return newInstance(rawClass, params, namespace, null);
    }

    /**
     * 生成实例
     *
     * @param name
     * @return
     */
    public static Object newInstance(String name) {
        return newInstance(name, null);
    }

    /**
     * 生成实例
     * 
     * @param name
     * @param assembler
     * @return
     */
    public static Object newInstance(String name, ClassAssembler assembler) {
        return newInstance(name, null, assembler);
    }

    /**
     * 集合填充生成指定类型对象
     * 
     * @param name
     * @param params
     * @param assembler
     * @return
     */
    public static Object newInstance(String name, Map<String, ? extends Object> params, ClassAssembler assembler) {
        return newInstance(Asserts.isNotNull(getClass(name), "Unable to instantiate class %s", name), params, assembler);
    }

    /**
     * 集合填充生成指定类型对象
     * 
     * @param params
     * @return
     */
    public static Object newInstance(Map<String, ? extends Object> params) {
        return newInstance(params, (ClassAssembler) null);
    }

    /**
     * 集合填充生成指定类型对象
     * 
     *
     * @param params
     * @param assembler
     * @return
     */
    public static Object newInstance(Map<String, ? extends Object> params, ClassAssembler assembler) {
        Object className = params.get(KEY_CLASS);
        if (Strings.isEmpty(className)) {
            return params;
        }
        params.remove(KEY_CLASS);
        Class<?> clazz = className instanceof Class ? (Class<?>) className : getClass(className.toString());
        Asserts.isNotNull(clazz, "Cannot instantiation class %s", className);
        return newInstance(clazz, params, assembler);
    }

    /**
     * 集合填充生成指定类型对象
     * 
     * @param rawClass
     * @param params
     * @return
     */
    public static <T> T newInstance(Class<T> rawClass, Map<String, ? extends Object> params) {
        return newInstance(rawClass, params, (ClassAssembler) null);
    }

    /**
     * 集合填充生成指定类型对象
     * 
     * @param clazz
     * @param params
     * @param namespace
     * @param assembler
     * @return
     */
    public static <T> T newInstance(Class<T> clazz, Map<String, ? extends Object> params, String namespace, ClassAssembler assembler) {
        return newInstance(clazz, Helpers.getMap(params, namespace), assembler);
    }

    /**
     * 集合填充生成指定类型对象
     *
     * @param rawClass
     * @param params
     * @param assembler
     * @return
     */
    public static <T> T newInstance(Class<T> rawClass, Map<String, ? extends Object> params, ClassAssembler assembler) {
        T instance = newInstance(rawClass, assembler);
        if (params != null && params.size() > 0) {
            toInstance(instance, params, assembler);
        }
        return instance;
    }

    /**
     * 生成实例
     * 
     * @param <T>
     * @param rawClass
     * @return
     */
    public static <T> T newInstance(Class<? extends T> rawClass) {
        return newInstance(rawClass, (ClassAssembler) null);
    }

    /**
     * 生成实例
     *
     * @param <T>
     * @param rawClass
     * @param assembler
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<? extends T> rawClass, ClassAssembler assembler) {
        if (assembler != null) {
            Class<?> pending = assembler.assemble(rawClass);
            if (pending != null) {
                rawClass = (Class<? extends T>) pending;
            }
        }
        if (isAbstract(rawClass)) {
            if (Collection.class.isAssignableFrom(rawClass)) {
                if (SortedSet.class.isAssignableFrom(rawClass)) {
                    return (T) new TreeSet<Object>();
                } else if (Set.class.isAssignableFrom(rawClass)) {
                    return (T) new LinkedHashSet<Object>();
                } else if (Queue.class.isAssignableFrom(rawClass)) {
                    return (T) new LinkedList<Object>();
                }
                return (T) new ArrayList<Object>();
            } else if (Map.class.isAssignableFrom(rawClass)) {
                if (SortedMap.class.isAssignableFrom(rawClass)) {
                    return (T) new LinkedHashMap<String, Object>();
                }
                return (T) new HashMap<String, Object>();
            }
        }
        Asserts.isNotNull(rawClass, "Cannot instantiation class");
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
    public static <T> T toInstance(T instance, Map<String, ? extends Object> params, ClassAssembler assembler) {
        if (instance instanceof Map) {
            ((Map<String, Object>) instance).putAll(params);
            return instance;
        }
        final Map<String, ? extends Object> data = Helpers.toHierarchical(params);
        Map<String, List<BoundField>> fields = getBoundFields(instance.getClass());
        assembler = assembler == null ? new AssemblerAdapter() : new AssemblerAdapter(assembler) {
            public Object assemble(BoundField field, Object instance, Object value, Object... args) {
                return super.assemble(field, instance, value, (Object[]) Helpers.newArray(args, data)); // 装配器二次封装：传递原始数据
            }
        };
        Object value;
        String key;
        BoundField field;
        for (Entry<String, List<BoundField>> entry : fields.entrySet()) {
            key = entry.getKey();
            field = entry.getValue().get(0);
            if (field.isBatch()) {
                field.setValue(instance, getValues(data, key, field.getAlias()), assembler);
            } else if ((value = getValue(data, key, field.getAlias())) != null) {
                field.setValue(instance, value, assembler);
            } else {
                field.setValue(instance, UNKNOWN, assembler);
            }
        }
        if (instance instanceof Extendable) {
            ((Extendable) instance).setExtends(params);
        }
        return instance;
    }

    private static Object getValues(Map<String, ? extends Object> params, String fieldName, String aliasName) {
        Object value = Helpers.getValues(params, fieldName);
        if (value == null) {
            value = Helpers.getValues(params, Strings.toFieldName(fieldName));
            if (value == null) {
                value = Helpers.getValues(params, Strings.toColumnName(fieldName));
                if (value == null) {
                    value = Helpers.getValues(params, aliasName);
                }
            }
        }
        return value;
    }

    private static Object getValue(Map<String, ? extends Object> params, String fieldName, String aliasName) {
        Object value = Helpers.getValue(params, fieldName);
        if (value == null) {
            value = Helpers.getValue(params, Strings.toFieldName(fieldName));
            if (value == null) {
                value = Helpers.getValue(params, Strings.toColumnName(fieldName));
                if (value == null) {
                    value = Helpers.getValue(params, aliasName);
                }
            }
        }
        return value;
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
        Object[] values = grid[1] instanceof Object[][] ? ((Object[][]) grid[1])[0] : (Object[]) grid[1];
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
    public static <T> T toBean(Class<T> clazz, Object[] columns, Object[] values) {
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
    public static <T> T toBean(Class<T> clazz, Object[] columns, Object[] values, ClassAssembler assembler) {
        return Reflects.newInstance(clazz, toMap(columns, values), assembler);
    }

    /**
     * 提取字段值转换为集合
     * 
     * @param bean
     * @return
     */
    public static Map<String, Object> toMap(Object bean) {
        return toMap(bean, false, false, Object.class);
    }

    /**
     * 提取字段值转换为集合
     * 
     * @param bean
     * @param desensitization
     * @return
     */
    public static Map<String, Object> toMap(Object bean, boolean desensitization) {
        return toMap(bean, desensitization, false, Object.class);
    }

    /**
     * 提取字段值转换为集合
     * 
     * @param bean
     * @param desensitization
     * @param deep
     * @return
     */
    public static Map<String, Object> toMap(Object bean, boolean desensitization, boolean deep) {
        return toMap(bean, desensitization, deep, Object.class);
    }

    /**
     * 提取字段值转换为集合
     * 
     * @param <T>
     * @param bean
     * @param clazz
     * @return
     */
    public static <T> Map<String, T> toMap(Object bean, Class<T> clazz) {
        return toMap(bean, false, false, clazz);
    }

    /**
     * 提取字段值转换为集合
     * 
     * @param <T>
     * @param bean
     * @param desensitization
     * @param clazz
     * @return
     */
    public static <T> Map<String, T> toMap(Object bean, boolean desensitization, Class<T> clazz) {
        return toMap(bean, desensitization, false, clazz);
    }

    /**
     * 提取字段值转换为集合
     * 
     * @param bean
     * @param desensitization
     * @param deep
     * @param clazz
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> toMap(Object bean, boolean desensitization, boolean deep, Class<T> clazz) {
        boolean translate = bean instanceof String; // 转义标记
        Object value;
        if (translate && Strings.isNotEmpty(value = Helpers.fromJson((String) bean, Map.class))) bean = value;
        Map<String, T> values = new HashMap<String, T>();
        Map<String, T> pending;
        Object translateValue;
        if (bean instanceof Map) {
            for (Entry<?, ?> entry : ((Map<?, ?>) bean).entrySet()) {
                value = entry.getValue();
                if (value != null) {
                    if (deep && clazz == Object.class) {
                        translateValue = pending = toMap(value, desensitization, deep, clazz);
                        if (!pending.isEmpty() || value instanceof String && Strings.isNotEmpty(translateValue = Helpers.fromJson((String) value))) value = translateValue;
                    } else if (String.class.isAssignableFrom(clazz)) {
                        value = value instanceof String ? String.valueOf(value) : Helpers.toJson(value, desensitization);
                    } else if (value instanceof String && !translate) {
                        value = Strings.empty(Helpers.fromJson((String) value), (String) value); // 对未转义对象进行转换
                    } else if (clazz != Object.class) {
                        value = Converter.convert(clazz, value);
                    }
                }
                values.put(String.valueOf(entry.getKey()), (T) value);
            }
        } else if (!(bean == null || Reflects.isGeneral(bean.getClass()))) {
            Map<String, List<BoundField>> fields = getBoundFields(bean.getClass());
            for (Entry<String, List<BoundField>> entry : fields.entrySet()) {
                try {
                    value = entry.getValue().get(0).getValue(bean, desensitization);
                    if (deep && clazz == Object.class) {
                        pending = toMap(value, desensitization, deep, clazz);
                        if (!pending.isEmpty()) {
                            value = pending;
                        }
                    } else if (String.class.isAssignableFrom(clazz)) {
                        value = Helpers.toJson(value, desensitization);
                    } else if (clazz != Object.class) {
                        value = Converter.convert(clazz, value);
                    }
                    values.put(entry.getKey(), (T) value);
                } catch (Exception e) {
                    LOG.warn("Failure getting field [{}] value.", entry.getKey(), e);
                }
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
        return toMap(grid, false);
    }

    /**
     * 根据网格模型转换为集合
     * 
     * @param grid
     * @param form
     * @return
     */
    public static Map<String, Object> toMap(Object[] grid, boolean form) {
        if (form) {
            Map<String, Object> result = new HashMap<String, Object>(grid.length);
            for (Object item : grid) {
                result.put(String.valueOf(((Object[]) item)[0]), ((Object[]) item)[1]);
            }
            return result;
        } else {
            return toMap((Object[]) grid[0], grid[1] instanceof Object[][] ? ((Object[][]) grid[1])[0] : (Object[]) grid[1]);
        }
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
    public static <T> List<T> toList(Class<T> clazz, Object[] grid, ClassAssembler assembler) {
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
    public static <T> List<T> toList(Class<T> clazz, List<Map<String, Object>> values) {
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
    public static <T> List<T> toList(Class<T> clazz, List<Map<String, Object>> values, Callable<T> interceptor) {
        List<T> result = new ArrayList<T>(values.size());
        int step = 0;
        T instance;
        for (Map<String, Object> value : values) {
            instance = interceptor.call(step++, newInstance(clazz, value), result);
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
    public static List<Map<String, Object>> toList(Object[] columns, Object[][] values) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(values.length);
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
            return Helpers.equals(pa.getOwnerType(), pb.getOwnerType()) && pa.getRawType().equals(pb.getRawType()) && Arrays.equals(pa.getActualTypeArguments(), pb.getActualTypeArguments());
        } else if (source instanceof GenericArrayType) {
            if (!(target instanceof GenericArrayType)) {
                return false;
            }
            GenericArrayType ga = (GenericArrayType) source;
            GenericArrayType gb = (GenericArrayType) target;
            return equals(ga.getGenericComponentType(), gb.getGenericComponentType());
        } else if (source instanceof WildcardType) {
            if (!(target instanceof WildcardType)) {
                return false;
            }
            WildcardType wa = (WildcardType) source;
            WildcardType wb = (WildcardType) target;
            return Arrays.equals(wa.getUpperBounds(), wb.getUpperBounds()) && Arrays.equals(wa.getLowerBounds(), wb.getLowerBounds());

        } else if (source instanceof TypeVariable) {
            if (!(target instanceof TypeVariable)) {
                return false;
            }
            TypeVariable<?> va = (TypeVariable<?>) source;
            TypeVariable<?> vb = (TypeVariable<?>) target;
            return va.getGenericDeclaration() == vb.getGenericDeclaration() && va.getName().equals(vb.getName());
        } else {
            return false;
        }
    }

    /**
     * 参数泛型
     * 
     * @author issing
     */
    private static final class ParameterizedTypeImpl implements ParameterizedType, Serializable {
        private static final long serialVersionUID = 5081438518083630676L;

        private final Type ownerType;

        private final Type rawType;

        private final Type[] typeArguments;

        public ParameterizedTypeImpl(Type ownerType, Type rawType, Type... typeArguments) {
            if (rawType instanceof Class<?>) {
                Class<?> rawClass = (Class<?>) rawType;
                Asserts.throwArgument(ownerType != null || rawClass.getEnclosingClass() == null);
                Asserts.throwArgument(ownerType == null || rawClass.getEnclosingClass() != null);
            }
            this.ownerType = ownerType == null ? null : Reflects.toCanonicalize(ownerType);
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
            return other instanceof ParameterizedType && Reflects.equals(this, (ParameterizedType) other);
        }

        public int hashCode() {
            return Arrays.hashCode(typeArguments) ^ rawType.hashCode() ^ Helpers.hashCode(ownerType);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(30 * (typeArguments.length + 1));
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
    private static final class GenericArrayTypeImpl implements GenericArrayType, Serializable {
        private static final long serialVersionUID = -1183771465139410856L;

        private final Type componentType;

        public GenericArrayTypeImpl(Type componentType) {
            this.componentType = toCanonicalize(componentType);
        }

        public Type getGenericComponentType() {
            return componentType;
        }

        public boolean equals(Object o) {
            return o instanceof GenericArrayType && Reflects.equals(this, (GenericArrayType) o);
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
    private static final class WildcardTypeImpl implements WildcardType, Serializable {
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
            return lowerBound != null ? new Type[] { lowerBound } : EMPTY_TYPE_ARRAY;
        }

        public boolean equals(Object other) {
            return other instanceof WildcardType && Reflects.equals(this, (WildcardType) other);
        }

        public int hashCode() {
            return (lowerBound != null ? 31 + lowerBound.hashCode() : 1) ^ (31 + upperBound.hashCode());
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

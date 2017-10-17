package net.isger.util.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import net.isger.util.Asserts;
import net.isger.util.Reflects;

public class TypeToken<T> {

    private Type type;

    private Class<? super T> rawClass;

    private int hashCode;

    protected TypeToken() {
        setToken(getActualType(getClass()));
    }

    private TypeToken(Type type) {
        setToken(type);
    }

    @SuppressWarnings("unchecked")
    private void setToken(Type type) {
        this.type = Reflects.toCanonicalize(Asserts.isNotNull(type));
        this.rawClass = (Class<? super T>) Reflects.getRawClass(this.type);
        this.hashCode = this.type.hashCode();
    }

    private Type getActualType(Class<?> type) {
        Type superType = type.getGenericSuperclass();
        Asserts.isInstance(Class.class, superType, "Missing type parameter");
        ParameterizedType paramType = (ParameterizedType) superType;
        return paramType.getActualTypeArguments()[0];
    }

    public final Class<? super T> getRawClass() {
        return rawClass;
    }

    public final Type getType() {
        return type;
    }

    public final int hashCode() {
        return this.hashCode;
    }

    public final boolean equals(Object o) {
        return o instanceof TypeToken<?>
                && Reflects.equals(type, ((TypeToken<?>) o).type);
    }

    public final String toString() {
        return Reflects.getName(type);
    }

    public static TypeToken<?> get(Type type) {
        return new TypeToken<Object>(type);
    }

    public static <T> TypeToken<T> get(Class<T> type) {
        return new TypeToken<T>(type);
    }

}

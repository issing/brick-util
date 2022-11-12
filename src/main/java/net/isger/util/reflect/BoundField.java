package net.isger.util.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import net.isger.util.Asserts;
import net.isger.util.Helpers;
import net.isger.util.Reflects;
import net.isger.util.Strings;
import net.isger.util.anno.Affix;
import net.isger.util.anno.Alias;
import net.isger.util.anno.Infect;
import net.isger.util.anno.Sensitive;

public class BoundField {

    private TypeToken<?> token;

    private Field field;

    private String name;

    private String alias;

    private String affix;

    private boolean sensitive;

    private boolean inject;

    private boolean infect;

    private boolean batch;

    @SuppressWarnings("unchecked")
    public BoundField(Field field) {
        TypeToken<?> declaring = TypeToken.get(field.getDeclaringClass());
        this.token = TypeToken.get(Reflects.getResolveType(declaring.getType(), declaring.getRawClass(), field.getGenericType()));
        this.field = field;
        this.field.setAccessible(true);
        this.name = field.getName();
        Affix affix = field.getAnnotation(Affix.class);
        if (affix != null) {
            this.affix = Strings.empty(affix.value());
        }
        Alias alias = field.getAnnotation(Alias.class);
        if (alias != null) {
            this.alias = Strings.empty(alias.value());
        } else if (Strings.isNotEmpty(this.affix)) {
            try {
                Map<String, Object> config = (Map<String, Object>) Helpers.fromJson(this.affix);
                this.alias = Strings.toFieldName((String) config.get("name"));
            } catch (Exception e) {
            }
        }
        this.sensitive = field.getAnnotation(Sensitive.class) != null;
        this.inject = field.getAnnotation(Inject.class) != null;
        this.infect = field.getAnnotation(Infect.class) != null;
        Type resolveType = token.getType();
        Class<?> rawClass = token.getRawClass();
        this.batch = resolveType instanceof GenericArrayType || Collection.class.isAssignableFrom(rawClass);
    }

    public TypeToken<?> getToken() {
        return token;
    }

    public Field getField() {
        return field;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public String getAffix() {
        return affix;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public boolean isInject() {
        return inject;
    }

    public boolean isInfect() {
        return infect;
    }

    public boolean isBatch() {
        return batch;
    }

    public void setValue(Object instance, Object value) {
        setValue(instance, value, null);
    }

    public void setValue(Object instance, Object value, ClassAssembler assembler) {
        try {
            FieldAssembler fieldAssembler = assembler == null ? null : assembler.getFieldAssembler();
            if (isInfect() && fieldAssembler != null) {
                value = fieldAssembler.assemble(this, instance, value);
            }
            if (value != Reflects.UNKNOWN) {
                Class<?> rawClass = token.getRawClass();
                if (rawClass.isInstance(value)) {
                    value = resolve(rawClass, token.getType(), value, assembler);
                } else {
                    try {
                        value = Converter.convert(token.getType(), value, assembler);
                    } catch (Exception e) {
                        value = Converter.defaultValue(token.getType());
                    }
                }
                field.set(instance, value);
            }
        } catch (Throwable e) {
            throw Asserts.state("Failure to setting field '%s' of %s: %s", getName(), field.getDeclaringClass(), value, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object resolve(Class<?> rawClass, Type resolveType, Object value, ClassAssembler assembler) {
        if (resolveType instanceof GenericArrayType) {
            resolveType = Reflects.getComponentType(resolveType);
            rawClass = Reflects.getRawClass(resolveType);
            int size = Array.getLength(value);
            Object array = Array.newInstance(rawClass, size);
            for (int i = 0; i < size; i++) {
                Array.set(array, i, resolve(rawClass, resolveType, Array.get(value, i), assembler));
            }
            value = array;
        } else if (Collection.class.isAssignableFrom(rawClass) && (value instanceof Collection)) {
            ParameterizedType paramType = (ParameterizedType) resolveType;
            Collection<Object> resolve = (Collection<Object>) Reflects.newInstance(rawClass, assembler);
            resolveType = paramType.getActualTypeArguments()[0];
            rawClass = Reflects.getClass(resolveType);
            for (Object instance : (Collection<?>) value) {
                resolve.add(resolve(rawClass, paramType.getActualTypeArguments()[0], instance, assembler));
            }
            value = resolve;
        } else if (resolveType instanceof Class && (!((Class<?>) resolveType).isInstance(value))) {
            value = Converter.convert((Class<?>) resolveType, value);
        }
        return value;
    }

    public Object getValue(Object instance) {
        return getValue(instance, false);
    }

    public Object getValue(Object instance, boolean desensitization) {
        try {
            return desensitization && isSensitive() ? null : field.get(instance);
        } catch (IllegalAccessException e) {
            throw Asserts.state("Can not to access field %s", getName(), e);
        }
    }

    public boolean match(String name) {
        return name.equals(this.name) || name.equals(alias);
    }

    public String toString() {
        int mod = field.getModifiers();
        return (((mod == 0) ? "" : (Modifier.toString(mod) + " ")) + token + " " + Reflects.getName(field.getDeclaringClass()) + "." + getName());
    }

}

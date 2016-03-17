package net.isger.util.reflect;

import java.lang.reflect.Field;

import net.isger.util.Strings;
import net.isger.util.anno.Alias;

public class BoundField {

    private Field field;

    private String name;

    private String aliasName;

    public BoundField(Field field) {
        this.field = field;
        this.field.setAccessible(true);
        this.name = field.getName();
        Alias alias = field.getAnnotation(Alias.class);
        if (alias != null) {
            this.aliasName = Strings.empty(alias.value());
        }
    }

    public Field getField() {
        return field;
    }

    public String getName() {
        return name;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setValue(Object instance, Object value) {
        Class<?> type = field.getType();
        try {
            if (!type.isInstance(value)) {
                value = Converter.convert(type, value);
            }
            field.set(instance, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failure to setting field '"
                    + getName() + "' of " + field.getDeclaringClass() + ": "
                    + value, e.getCause());
        }
    }

    public Object getValue(Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Can not to access field "
                    + getName(), e);
        }
    }

    public boolean match(String name) {
        return name.equals(this.name) || name.equals(aliasName);
    }

}

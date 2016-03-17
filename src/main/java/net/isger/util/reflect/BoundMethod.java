package net.isger.util.reflect;

import java.lang.reflect.Method;

import net.isger.brick.blue.Marks.TYPE;
import net.isger.util.Reflects;
import net.isger.util.Strings;
import net.isger.util.anno.Alias;

public class BoundMethod {

    private Method method;

    private String name;

    private String aliasName;

    private String methodName;

    public BoundMethod(Method method) {
        this.method = method;
        this.method.setAccessible(true);
        this.name = method.getName();
        Alias alias = method.getAnnotation(Alias.class);
        if (alias != null) {
            this.aliasName = Strings.empty(alias.value());
        }
        this.methodName = makeName(method);
    }

    public Method getMethod() {
        return method;
    }

    public String getName() {
        return name;
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getMethodName() {
        return methodName;
    }

    public boolean isAbstract() {
        return Reflects.isAbstract(method);
    }

    public Object invoke(Object instance, Object... args) {
        try {
            return method.invoke(instance, args);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("Failure to invoke method "
                    + getName(), cause);
        }
    }

    public static String makeName(Method method) {
        return makeName(method.getName(), method.getReturnType(),
                method.getParameterTypes());
    }

    public static String makeName(String name) {
        return makeName(name, Void.TYPE);
    }

    public static String makeName(String name, Class<?> resultType,
            Class<?>... argTypes) {
        return name + TYPE.getMethDesc(resultType, argTypes);
    }

}

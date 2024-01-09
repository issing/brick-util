package net.isger.util.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import net.isger.brick.blue.Marks.TYPE;
import net.isger.util.Asserts;
import net.isger.util.Reflects;
import net.isger.util.Strings;
import net.isger.util.anno.Affix;
import net.isger.util.anno.Alias;

public class BoundMethod {

    private Method method;

    private String name;

    private String aliasName;

    private String methodDesc;

    private String affix;

    public BoundMethod(Method method) {
        this.method = method;
        this.method.setAccessible(true);
        this.name = method.getName();
        Alias alias = method.getAnnotation(Alias.class);
        if (alias != null) {
            this.aliasName = Strings.empty(alias.value());
        }
        this.methodDesc = makeMethodDesc(method);
        Affix affix = method.getAnnotation(Affix.class);
        if (affix != null) {
            this.affix = Strings.empty(affix.value());
        }
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

    public String getMethodDesc() {
        return methodDesc;
    }

    public String getAffix() {
        return affix;
    }

    public boolean isAbstract() {
        return Reflects.isAbstract(method);
    }

    public <T extends Annotation> T getAnnotation(Class<T> anno) {
        return method.getAnnotation(anno);
    }

    public Object invoke(Object instance, Object... args) {
        try {
            return method.invoke(instance, args);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw Asserts.state("Failure to invoke method %s", getName(), cause);
        }
    }

    public static String makeMethodDesc(Method method) {
        return makeMethodDesc(method.getName(), method.getReturnType(), method.getParameterTypes());
    }

    public static String makeMethodDesc(String name) {
        return makeMethodDesc(name, Void.TYPE);
    }

    public static String makeMethodDesc(String name, Class<?> resultType, Class<?>... argTypes) {
        if (resultType == null || resultType == Void.class) {
            resultType = Void.TYPE;
        }
        return isMethodDesc(name) ? name : name + TYPE.getMethDesc(resultType, argTypes);
    }

    public static boolean matches(String methodName, String name) {
        return methodName.matches(name + TYPE.REGEX_METH);
    }

    public static boolean isMethodDesc(String methodName) {
        return Strings.endWithIgnoreCase(methodName, TYPE.REGEX_METH);
    }

    public static String getName(String methodName) {
        int index = methodName.lastIndexOf("(");
        if (index > 0) {
            methodName = methodName.substring(0, index);
        }
        return methodName;
    }

}

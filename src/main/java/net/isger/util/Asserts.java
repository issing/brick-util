package net.isger.util;

import java.lang.reflect.Type;

/**
 * 断言工具
 * 
 * @author issing
 *
 */
public class Asserts {

    private Asserts() {
    }

    public static void isFalse(boolean expression) {
        isFalse(expression, "The expression must be false");
    }

    public static void isFalse(boolean expression, String message,
            Object... args) {
        isTrue(!expression, message, args);
    }

    public static void isTrue(boolean expression) {
        isTrue(expression, "The expression must be true");
    }

    public static void isTrue(boolean expression, String message,
            Object... args) {
        throwArgument(expression, message, args);
    }

    public static <T extends Object> T isNull(T value) {
        return isNull(value, "The argument must be null");
    }

    public static <T extends Object> T isNull(T value, String message,
            Object... args) {
        throwArgument(value == null, message, args);
        return value;
    }

    public static <T extends Object> T isNotNull(T value) {
        return isNotNull(value, "The argument not be null");
    }

    public static <T extends Object> T isNotNull(T value, String message,
            Object... args) {
        throwArgument(value != null, message,
                (Object[]) Helpers.newArray(value, args));
        return value;
    }

    public static <T extends Object> String isEmpty(T value) {
        return isEmpty(value, "The argument must be null or empty");
    }

    public static <T extends Object> String isEmpty(T value, String message,
            Object... args) {
        throwArgument(Strings.isEmpty(value), message, args);
        return "";
    }

    public static <T extends Object> String isNotEmpty(T value) {
        return isNotEmpty(value, "The argument not be null or empty");
    }

    public static <T extends Object> String isNotEmpty(T value, String message,
            Object... args) {
        throwArgument(Strings.isNotEmpty(value), message,
                (Object[]) Helpers.newArray(value, args));
        return Strings.empty(value.toString());
    }

    public static String isNotContains(String source, String value) {
        return isNotContains(source, value,
                "The source must not contain the substring [%s]", value);
    }

    public static String isNotContains(String source, String value,
            String message, Object... args) {
        throwArgument(Strings.isEmpty(source) || Strings.isEmpty(value)
                || !source.contains(value), message, args);
        return source;
    }

    public static void isInstance(Class<?> clazz, Object instance) {
        isInstance(clazz, instance, "");
    }

    public static void isInstance(Class<?> type, Object instance,
            String message, Object... args) {
        isNotNull(type, "Type to check against must not be null");
        throwArgument(type.isInstance(instance),
                "%sinstance of class [%s] must be an instance of %s",
                Strings.isEmpty(message) ? ""
                        : Strings.format(message, args) + " - ",
                (instance != null ? instance.getClass().getName() : "null"),
                type);
    }

    public static void isAssignable(Class<?> superType, Class<?> subType) {
        isAssignable(superType, subType, null);
    }

    public static void isAssignable(Class<?> superType, Class<?> subType,
            String message, Object... args) {
        isNotNull(superType, "Type to check against must not be null");
        throwArgument(superType.isAssignableFrom(subType),
                "%s%s is not assignable to %s",
                Strings.isEmpty(message) ? ""
                        : Strings.format(message, args) + " - ",
                subType, superType);
    }

    public static void isNotPrimitive(Type type) {
        throwArgument(
                !(type instanceof Class<?>) || !((Class<?>) type).isPrimitive(),
                "%s is primitive", type);
    }

    public static void throwArgument(boolean expression) {
        throwArgument(expression, null);
    }

    public static void throwArgument(boolean expression, String message,
            Object... args) {
        if (!expression) {
            throw argument(message, args);
        }
    }

    public static IllegalArgumentException argument(String message,
            Object... args) {
        if (Strings.isEmpty(message)) {
            message = "The argument is invalid";
        }
        return new IllegalArgumentException(
                "(X) " + Strings.format(message, args), getCause(args));
    }

    public static void throwState(boolean expression) {
        throwState(expression, null);
    }

    public static void throwState(boolean expression, String message,
            Object... args) {
        if (!expression) {
            throw state(message, args);
        }
    }

    public static IllegalStateException state(String message, Object... args) {
        if (Strings.isEmpty(message)) {
            message = "The state invariant must be true";
        }
        return new IllegalStateException("(X) " + Strings.format(message, args),
                getCause(args));
    }

    public static Throwable getCause(Object[] args) {
        return args != null && args.length > 0
                && (args[args.length - 1] instanceof Throwable)
                        ? (Throwable) args[args.length - 1]
                        : null;
    }

}

package net.isger.util;

/**
 * 断言工具
 * 
 * @author issing
 *
 */
public class Asserts {

    private Asserts() {
    }

    public static void isTrue(boolean expression) {
        isTrue(expression, "The expression must be true");
    }

    public static void isTrue(boolean expression, String message,
            Object... args) {
        throwArgument(expression, message, args);
    }

    public static void isNull(Object object) {
        isNull(object, "The argument must be null");
    }

    public static void isNull(Object object, String message, Object... args) {
        throwArgument(object == null, message, args);
    }

    public static void isNotNull(Object object) {
        isNotNull(object, "The argument not be null");
    }

    public static void isNotNull(Object object, String message,
            Object... args) {
        throwArgument(object != null, message, args);
    }

    public static void isNotEmpty(String text) {
        isNotEmpty(text, "The argument not be null or empty");
    }

    public static void isNotEmpty(String text, String message, Object... args) {
        throwArgument(Strings.isNotEmpty(text), message, args);
    }

    public static void isNotContains(String source, String value) {
        isNotContains(source, value,
                "The source must not contain the substring [%s]", value);
    }

    public static void isNotContains(String source, String value,
            String message, Object... args) {
        throwArgument(Strings.isEmpty(source) || Strings.isEmpty(value)
                || !source.contains(value), message, args);
    }

    public static void isInstance(Class<?> clazz, Object obj) {
        isInstance(clazz, obj, "");
    }

    public static void isInstance(Class<?> type, Object obj, String message,
            Object... args) {
        isNotNull(type, "Type to check against must not be null");
        throwArgument(type.isInstance(obj),
                "%sinstance of class [%s] must be an instance of %s",
                Strings.isEmpty(message) ? ""
                        : Strings.format(message, args) + " - ",
                (obj != null ? obj.getClass().getName() : "null"), type);
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
                        ? (Throwable) args[args.length - 1] : null;
    }

}

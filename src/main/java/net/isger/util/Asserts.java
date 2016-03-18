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
        argument(expression, message, args);
    }

    public static void isNull(Object object) {
        isNull(object, "The argument must be null");
    }

    public static void isNull(Object object, String message, Object... args) {
        argument(object == null, message, args);
    }

    public static void isNotNull(Object object) {
        isNotNull(object, "The argument not be null");
    }

    public static void isNotNull(Object object, String message, Object... args) {
        argument(object != null, message, args);
    }

    public static void isNotEmpty(String text) {
        isNotEmpty(text, "The argument not be null or empty");
    }

    public static void isNotEmpty(String text, String message, Object... args) {
        argument(Strings.isNotEmpty(text), message, args);
    }

    public static void isNotContains(String source, String value) {
        isNotContains(source, value,
                "The source must not contain the substring [%s]", value);
    }

    public static void isNotContains(String source, String value,
            String message, Object... args) {
        argument(
                Strings.isEmpty(source) || Strings.isEmpty(value)
                        || !source.contains(value), message, args);
    }

    public static void isInstance(Class<?> clazz, Object obj) {
        isInstance(clazz, obj, "");
    }

    public static void isInstance(Class<?> type, Object obj, String message,
            Object... args) {
        isNotNull(type, "Type to check against must not be null");
        argument(type.isInstance(obj),
                "%sinstance of class [%s] must be an instance of %s",
                Strings.isEmpty(message) ? "" : Strings.format(message, args)
                        + " - ", (obj != null ? obj.getClass().getName()
                        : "null"), type);
    }

    public static void isAssignable(Class<?> superType, Class<?> subType) {
        isAssignable(superType, subType, null);
    }

    public static void isAssignable(Class<?> superType, Class<?> subType,
            String message, Object... args) {
        isNotNull(superType, "Type to check against must not be null");
        argument(superType.isAssignableFrom(subType),
                "%s%s is not assignable to %s", Strings.isEmpty(message) ? ""
                        : Strings.format(message, args) + " - ", subType,
                superType);
    }

    public static void argument(boolean expression) {
        argument(expression, "The argument is invalid");
    }

    public static void argument(boolean expression, String message,
            Object... args) {
        if (!expression) {
            throw new IllegalArgumentException("(X) "
                    + Strings.format(message, args));
        }
    }

    public static void state(boolean expression) {
        state(expression, "The state invariant must be true");
    }

    public static void state(boolean expression, String message, Object... args) {
        if (!expression) {
            throw new IllegalStateException("(X) "
                    + Strings.format(message, args));
        }
    }

}

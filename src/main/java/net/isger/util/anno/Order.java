package net.isger.util.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 排序
 * 
 * @author issing
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface Order {

    public static final int HIGH_PRECEDENCE = Integer.MIN_VALUE;

    public static final int DEFAULT_PRECEDENCE = 0;

    public static final int LOW_PRECEDENCE = Integer.MAX_VALUE;

    int value() default DEFAULT_PRECEDENCE;

}

package net.isger.util.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 适配器注解
 * 
 * @author issing
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface Adapter {

    /** 默认方式 */
    public static final String DEFAULT = "";

    /**
     * 适配器名称
     * 
     * @return
     */
    String value() default DEFAULT;

}

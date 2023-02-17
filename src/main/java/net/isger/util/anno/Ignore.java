package net.isger.util.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略
 * 
 * @author issing
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface Ignore {

    public enum Mode {

        /** 排除 */
        EXCLUDE,

        /** 引入 */
        INCLUDE;

        public static final String EXCLUDE_NAME = "exclude";

        public static final String INCLUDE_NAME = "include";

    }

    /**
     * 忽略模式
     * 
     * @return
     */
    public Mode mode() default Mode.EXCLUDE;

    /**
     * 序列化
     * 
     * @return
     */
    public boolean serialize() default true;

}

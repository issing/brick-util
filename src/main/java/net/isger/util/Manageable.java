package net.isger.util;

/**
 * 可控接口
 * 
 * @author issing
 */
public interface Manageable {

    public static final int UNINITIALIZED = 0;

    public static final int INITIALIZING = 1;

    public static final int INITIALIZED = 2;

    public static final int DESTROYED = 3;

    /**
     * 初始
     */
    public void initial();

    /**
     * 注销
     */
    public void destroy();

}

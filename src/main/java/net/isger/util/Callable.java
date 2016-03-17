package net.isger.util;

/**
 * 回调接口
 * 
 * @author issing
 *
 */
public abstract class Callable<T> implements java.util.concurrent.Callable<T> {

    public T call() {
        return call(new Object[0]);
    }

    /**
     * 回调
     * 
     * @param args
     * @return
     */
    public abstract T call(Object... args);

}

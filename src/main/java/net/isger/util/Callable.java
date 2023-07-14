package net.isger.util;

/**
 * 回调接口
 * 
 * @author issing
 */
public class Callable<T> implements java.util.concurrent.Callable<T> {

    private static final Object[] NONE = new Object[0];

    /**
     * 回调
     */
    public T call() {
        return call(NONE);
    }

    /**
     * 回调
     * 
     * @param args
     * @return
     */
    public T call(Object... args) {
        T result = null;
        if (args != NONE) {
            result = call();
        }
        return result;
    }

    /**
     * 执行接口
     * 
     * @author issing
     */
    public static class Runnable extends Callable<Void> implements java.lang.Runnable {

        /**
         * 执行
         */
        public void run() {
            run(NONE);
        }

        /**
         * 回调
         */
        public final Void call() {
            run();
            return null;
        }

        /**
         * 回调
         * 
         * @param args
         * @return
         */
        public final Void call(Object... args) {
            run(args);
            return null;
        }

        /**
         * 执行
         *
         * @param args
         */
        public void run(Object... args) {
            if (args != NONE) {
                run();
            }
        }

    }

}

package net.isger.util;

/**
 * 回调接口
 * 
 * @author issing
 *
 */
public class Callable<T> implements java.util.concurrent.Callable<T> {

    public T call() {
        return call(new Object[0]);
    }

    /**
     * 回调
     * 
     * @param args
     * @return
     */
    public T call(Object... args) {
        return null;
    }

    /**
     * 回调接口
     * 
     * @author issing
     *
     */
    public static class Runnable extends Callable<Void> implements java.lang.Runnable {

        public void run() {
            call();
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
        }

    }

}

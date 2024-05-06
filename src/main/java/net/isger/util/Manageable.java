package net.isger.util;

/**
 * 管理接口
 * 
 * @author issing
 */
public interface Manageable {

    /**
     * 就绪
     * 
     * @return
     */
    public boolean hasReady();

    /**
     * 状态
     * 
     * @return
     */
    public Status getStatus();

    /**
     * 初始
     */
    public void initial();

    /**
     * 注销
     */
    public void destroy();

    /**
     * 管理状态
     * 
     * @author issing
     */
    public static enum Status {

        UNINITIALIZED(0), INITIALIZING(1), PENDING(2), INITIALIZED(3), DESTROYED(4), STATELESS(9);

        public static final int COUNT = 5;

        public final int value;

        private Status(int value) {
            this.value = value;
        }

    }
}

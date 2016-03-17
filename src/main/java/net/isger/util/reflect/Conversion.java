package net.isger.util.reflect;

public interface Conversion {

    /**
     * 支持类型
     * 
     * @param type
     * @return
     */
    public boolean isSupport(Class<?> type);

    /**
     * 转换类型
     * 
     * @param type
     * @param res
     * @return
     */
    public Object convert(Class<?> type, Object res);

}

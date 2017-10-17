package net.isger.util.reflect.conversion;

import java.lang.reflect.Type;

public interface Conversion {

    /**
     * 支持类型
     * 
     * @param type
     * @return
     */
    public boolean isSupport(Type type);

    /**
     * 转换类型
     * 
     * @param type
     * @param res
     * @return
     */
    public Object convert(Type type, Object value);

}

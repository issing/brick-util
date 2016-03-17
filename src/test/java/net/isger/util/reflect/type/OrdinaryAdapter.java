package net.isger.util.reflect.type;

import java.util.Collection;
import java.util.Map;

import net.isger.util.Reflects;

public class OrdinaryAdapter {

    /**
     * 支持适配类型
     * 
     * @param type
     * @return
     */
    public boolean isSupport(Class<?> type) {
        // 不支持数组、集合、列表
        return !(type.isArray() || Map.class.isAssignableFrom(type) || Collection.class
                .isAssignableFrom(type));
    }

    @SuppressWarnings("unchecked")
    public Object convert(Class<?> type, Object value) {
        Object result = null;
        Class<?> valueType = value.getClass();
        // 所属实例直接赋值
        if (type.isInstance(valueType)) {
            result = value;
        }
        // 转换为字符串
        else if (String.class.isAssignableFrom(type)) {
            result = value instanceof String ? value : String.valueOf(value);
        }
        // 转换为类对象
        else if (Class.class.isAssignableFrom(type)) {
            try {
                result = value instanceof Class ? value
                        : value instanceof String ? Class
                                .forName((String) value) : null;
            } catch (ClassNotFoundException e) {
            }
        }
        // 转换为对象（值为集合）
        else if (Map.class.isAssignableFrom(valueType)) {
            result = Reflects.newInstance(type, (Map<String, Object>) value);
        }
        return result;
    }

}

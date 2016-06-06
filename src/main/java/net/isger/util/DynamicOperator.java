package net.isger.util;

import java.util.List;
import java.util.Map;

import net.isger.util.anno.Ignore;
import net.isger.util.reflect.BoundMethod;

@Ignore
public class DynamicOperator implements Operator {

    /** 操作源 */
    private Object source;

    public DynamicOperator() {
        this.source = this;
    }

    public DynamicOperator(Object source) {
        Asserts.isNotNull(source, "The operator source not be null");
        this.source = source;
    }

    /**
     * 获取当前实例所有绑定方法
     * 
     * @return
     */
    private Map<String, List<BoundMethod>> getMethods() {
        return Reflects.getBoundMethods(getSource().getClass());
    }

    /**
     * 获取当前实例指定绑定方法
     * 
     * @param name
     * @return
     */
    private BoundMethod getMethod(String name) {
        return getMethods().get(name).get(0);
    }

    /**
     * 操作检测
     * 
     * @param name
     * @return
     */
    protected boolean hasOperate(String name) {
        return getMethods().containsKey(name);
    }

    public void operate() {
    }

    /**
     * 本实例指定绑定方法操作
     * 
     * @param operate
     */
    public Object operate(String operate) {
        return getMethod(BoundMethod.makeMethodName(operate)).invoke(getSource());
    }

    /**
     * 本实例指定绑定方法操作
     * 
     * @param operate
     * @param args
     */
    public Object operate(String operate, Object... args) {
        return getMethod(operate).invoke(getSource(), args);
    }

    /**
     * 获取操作源（子类操进行克隆操作需考虑重写本方法）
     * 
     * @return
     */
    protected Object getSource() {
        return source;
    }

    /**
     * 克隆实例
     */
    public Object clone() {
        DynamicOperator operator;
        try {
            operator = (DynamicOperator) super.clone();
            if (this == getSource()) {
                operator.source = operator;
            }
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e.getMessage(), e.getCause());
        }
        return operator;
    }

}

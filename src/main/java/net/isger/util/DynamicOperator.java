package net.isger.util;

import java.util.List;
import java.util.Map;

import net.isger.util.anno.Ignore;
import net.isger.util.reflect.BoundMethod;

/**
 * 动态操作器
 * 
 * @author issing
 *
 */
@Ignore
public class DynamicOperator implements Operator {

    /** 默认操作 */
    protected static final String METH_OPERATE;

    /** 操作源 */
    private Object source;

    static {
        METH_OPERATE = BoundMethod.makeMethodDesc("operate");
    }

    public DynamicOperator() {
        source = this;
    }

    public DynamicOperator(Object source) {
        Asserts.isNotNull(source, "The operator source not be null");
        this.source = source;
    }

    /**
     * 获取所有绑定方法
     * 
     * @return
     */
    private Map<String, List<BoundMethod>> getMethods() {
        return Reflects.getBoundMethods(getSource().getClass(), true);
    }

    /**
     * 获取指定绑定方法
     * 
     * @param operate
     * @return
     */
    protected final BoundMethod getMethod(String operate) {
        Map<String, List<BoundMethod>> methods = getMethods();
        if (methods.containsKey(operate)) {
            return methods.get(operate).get(0);
        }
        /* 非方法描述操作，尝试获取无参无返回值方法 */
        else if (!BoundMethod.isMethodDesc(operate)) {
            return getMethod(BoundMethod.makeMethodDesc(operate));
        }
        return null;
    }

    /**
     * 默认绑定方法操作
     */
    public void operate() {
        Asserts.throwState(getSource() != this, "No target operation");
        operate(METH_OPERATE);
    }

    /**
     * 指定绑定方法操作
     * 
     * @param operate
     * @param args
     */
    protected Object operate(String operate, Object... args) {
        BoundMethod method = getMethod(operate);
        Asserts.isNotNull(method, "Unfound the specified operate in the dynamic Operator [%s]", source.getClass().getName());
        return method.invoke(getSource(), args);
    }

    /**
     * 获取操作源（子类操进行克隆操作需考虑重写本方法）
     * 
     * @return
     */
    protected final Object getSource() {
        return source;
    }

    /**
     * 克隆实例
     */
    public Object clone() {
        DynamicOperator operator;
        try {
            operator = (DynamicOperator) super.clone();
            /* 重定向本源实例 */
            if (this == getSource()) {
                operator.source = operator;
            }
        } catch (CloneNotSupportedException e) {
            throw Asserts.state("Failure to clone operator", e);
        }
        return operator;
    }

}

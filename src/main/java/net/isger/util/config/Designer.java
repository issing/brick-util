package net.isger.util.config;

import java.util.Map;

/**
 * 设计器接口
 * 
 * @author issing
 *
 */
public interface Designer {

    /**
     * 设计
     * 
     * @param config
     */
    public void design(Map<String, Object> config);

}

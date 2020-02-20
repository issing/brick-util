package net.isger.util.load;

import net.isger.util.reflect.ClassAssembler;

/**
 * 加载器
 * 
 * @author issing
 * 
 */
public interface Loader {

    /**
     * 加载资源
     * 
     * @param res
     * @param assembler
     * @return
     */
    public Object load(Object res, ClassAssembler assembler);

}

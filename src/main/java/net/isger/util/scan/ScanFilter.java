package net.isger.util.scan;

import java.io.File;

/**
 * 扫描过滤器
 * 
 * @author issing
 *
 */
public interface ScanFilter {

    /**
     * 是否深度扫描
     * 
     * @param root
     * @param path
     * @return
     */
    public boolean isDeep(File root, File path);

    /**
     * 扫描过滤
     * 
     * @param name
     * @return
     */
    public boolean accept(String name);

}

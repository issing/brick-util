package net.isger.util.scan;

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
     * @return
     */
    public boolean isDeep();

    /**
     * 扫描过滤
     * 
     * @param name
     * @return
     */
    public boolean accept(String name);

}

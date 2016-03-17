package net.isger.util.scanner;

import java.net.URL;
import java.util.List;

/**
 * 扫描接口
 * 
 * @author issing
 *
 */
public interface Scan {

    /**
     * 扫描
     * 
     * @param url
     * @param filter
     * @return
     */
    public List<String> scan(URL url, ScanFilter filter);

}

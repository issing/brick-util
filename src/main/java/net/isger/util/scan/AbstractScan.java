package net.isger.util.scan;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * 抽象扫描类
 * 
 * @author issing
 *
 */
public abstract class AbstractScan implements Scan {

    /**
     * 扫描
     */
    public List<String> scan(URL url, ScanFilter filter) {
        if (!url.getProtocol().equalsIgnoreCase(getProtocol()))
            return null;
        return scan(url.getPath(), filter);
    }

    /**
     * 获取协议
     * 
     * @return
     */
    protected abstract String getProtocol();

    /**
     * 扫描资源
     * 
     * @param path
     * @param filter
     * @return
     */
    public abstract List<String> scan(String path, ScanFilter filter);

    /**
     * 目标名称
     * 
     * @param basePath
     * @param target
     * @return
     */
    protected String getName(File basePath, File target) {
        return target.getAbsolutePath().substring(
                basePath.getAbsolutePath().length());
    }

    public String toString() {
        return getProtocol();
    }

}

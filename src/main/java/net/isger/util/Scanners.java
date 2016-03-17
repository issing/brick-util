package net.isger.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import net.isger.util.hitch.Director;
import net.isger.util.scanner.FileScan;
import net.isger.util.scanner.JarScan;
import net.isger.util.scanner.Scan;
import net.isger.util.scanner.ScanFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 扫描器工具
 * 
 * @author issing
 *
 */
public class Scanners {

    private static final String KEY_SCANS = "brick.util.scans";

    private static final String SCAN_PATH = "net/isger/util/scan";

    private static final Logger LOG;

    private static final Scanners SCANNER;

    private Map<String, Scan> scans;

    static {
        LOG = LoggerFactory.getLogger(Scanners.class);
        SCANNER = new Scanners();
        addScan(new FileScan());
        addScan(new JarScan());
        new Director() {
            protected String directPath() {
                return directPath(KEY_SCANS, SCAN_PATH);
            }
        }.direct(SCANNER);
    }

    private Scanners() {
        scans = new Hashtable<String, Scan>();
    }

    public static void addScan(Scan scan) {
        String name = scan.getClass().getName();
        if (LOG.isDebugEnabled()) {
            LOG.info("Achieve scan [{}]", name);
        }
        scan = SCANNER.scans.put(name, scan);
        if (scan != null && LOG.isDebugEnabled()) {
            LOG.warn("(!) Discard scan [{}]", scan);
        }
    }

    public static List<String> scan(String name, ScanFilter filter) {
        List<String> result = null;
        for (URL url : Reflects.getResources(SCANNER, name)) {
            result = Helpers.getMerge(result, scan(url, filter));
        }
        return result == null ? new ArrayList<String>() : result;
    }

    private static List<String> scan(URL url, ScanFilter filter) {
        List<String> result = null;
        for (Scan scan : SCANNER.scans.values()) {
            result = Helpers.getMerge(result, scan.scan(url, filter));
        }
        return result == null ? new ArrayList<String>() : result;
    }

}

package net.isger.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import net.isger.util.hitch.Director;
import net.isger.util.scan.FileScan;
import net.isger.util.scan.JarScan;
import net.isger.util.scan.Scan;
import net.isger.util.scan.ScanFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 扫描器工具
 * 
 * @author issing
 *
 */
public class Scans {

    private static final String KEY_SCANS = "brick.util.scans";

    private static final String SCAN_PATH = "net/isger/util/scan";

    private static final Logger LOG;

    private static final Scans SCANNER;

    private Map<String, Scan> scans;

    static {
        LOG = LoggerFactory.getLogger(Scans.class);
        SCANNER = new Scans();
        addScan(new FileScan());
        addScan(new JarScan());
        new Director() {
            protected String directPath() {
                return directPath(KEY_SCANS, SCAN_PATH);
            }
        }.direct(SCANNER);
    }

    private Scans() {
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
        List<String> result = new ArrayList<String>();
        for (URL url : Reflects.getResources(SCANNER, name)) {
            Helpers.add(result, scan(url, filter));
        }
        return result;
    }

    private static List<String> scan(URL url, ScanFilter filter) {
        List<String> result = new ArrayList<String>();
        for (Scan scan : SCANNER.scans.values()) {
            Helpers.add(result, scan.scan(url, filter));
        }
        return result;
    }
}

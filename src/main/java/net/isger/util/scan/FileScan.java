package net.isger.util.scan;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileScan extends AbstractScan {

    private static final String PROTOCOL = "file";

    protected String getProtocol() {
        return PROTOCOL;
    }

    // public List<String> scan(URL url, final ScanFilter filter) {
    // if (!PROTOCOL.equalsIgnoreCase(url.getProtocol()))
    // return null;
    // List<String> result = null;
    // File path = new File(url.getPath());
    // if (path.isDirectory()) {
    // result = scan(path, path, filter);
    // }
    // return result;
    // }

    public List<String> scan(String path, ScanFilter filter) {
        List<String> result = null;
        File root = new File(path);
        if (root.isDirectory()) {
            result = scan(root, root, filter);
        }
        return result;
    }

    private List<String> scan(File root, File path, ScanFilter filter) {
        List<String> result = new ArrayList<String>();
        File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    if (filter.accept(file.getName())) {
                        result.add(getName(root, file));
                    }
                } else if (file.isDirectory() && filter.isDeep(root, file)) {
                    result.addAll(scan(root, file, filter));
                }
            }
        }
        return result;
    }

}

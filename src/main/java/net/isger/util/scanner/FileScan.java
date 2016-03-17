package net.isger.util.scanner;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FileScan extends AbstractScan {

    private static final String PROTOCOL = "file";

    public List<String> scan(URL url, final ScanFilter filter) {
        if (!PROTOCOL.equalsIgnoreCase(url.getProtocol()))
            return null;
        List<String> result = null;
        File path = new File(url.getPath());
        if (path.isDirectory()) {
            result = scan(path, path, filter);
        }
        return result;
    }

    private List<String> scan(File root, File path, ScanFilter filter) {
        List<String> result = new ArrayList<String>();
        boolean isDeep = filter.isDeep();
        File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    if (filter.accept(file.getName())) {
                        result.add(getName(root, file));
                    }
                } else if (file.isDirectory() && isDeep) {
                    result.addAll(scan(root, file, filter));
                }
            }
        }
        return result;
    }

    public String toString() {
        return PROTOCOL;
    }
}

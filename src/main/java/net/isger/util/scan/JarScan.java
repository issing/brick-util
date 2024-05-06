package net.isger.util.scan;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.isger.util.Files;

public class JarScan extends AbstractScan {

    private static final String PROTOCOL = "jar";

    private static final Logger LOG;

    static {
        LOG = LoggerFactory.getLogger(JarScan.class);
    }

    protected String getProtocol() {
        return PROTOCOL;
    }

    public List<String> scan(String path, ScanFilter filter) {
        File workPath = null;
        int index = path.indexOf("jar!/"); // 需要考虑“jar”嵌套问题（“jar”里面还存在“jar”）
        if (index != -1) {
            workPath = new File(path.substring(index + 5));
            path = path.substring(0, index + 3);
        } else if (!path.endsWith(".jar")) {
            throw new IllegalStateException("Have the ability to give " + path + " of jar");
        }

        List<String> result = new ArrayList<String>();
        ZipInputStream zis = null;
        try {
            zis = Files.openJarIS(path);
            ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) {
                if (match(workPath, entry, filter)) {
                    result.add(getName(workPath, new File(entry.getName())));
                }
            }
        } catch (IOException e) {
            LOG.warn("Error scanning hicher from path {}", path, e);
        } finally {
            Files.close(zis);
        }
        return result;
    }

    protected boolean match(File path, ZipEntry entry, ScanFilter filter) {
        boolean isMatch;
        if (isMatch = !entry.isDirectory()) {
            File sourceFile = new File(entry.getName());
            File parentPath = sourceFile.getParentFile();
            // toMatchPath: {
            if (parentPath == null) {
                isMatch = path == null;
            } else if (parentPath.getAbsolutePath().startsWith(path.getAbsolutePath())) {
                if (!filter.isDeep(path, parentPath)) {
                    isMatch = parentPath.equals(path);
                }
            } else {
                return false;
            }
            // isMatch = filter.isDeep(parentPath.getPath())
            // ? parentPath.getAbsolutePath()
            // .startsWith(path.getAbsolutePath())
            // : parentPath.equals(path);
            // }
            return isMatch && filter.accept(sourceFile.getName());
        }
        return isMatch;
    }

}

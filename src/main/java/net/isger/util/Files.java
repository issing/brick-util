package net.isger.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * 文件工具
 * 
 * @author issing
 * 
 */
public class Files {

    private Files() {
    }

    /**
     * 路径转换
     * 
     * @param value
     * @return
     */
    public static String toPath(String value) {
        return value.replaceAll("[.]|\\\\", "/");
    }

    /**
     * 路径转换
     * 
     * @param value
     * @param name
     * @return
     */
    public static String toPath(String value, String name) {
        String path = toPath(value);
        if (!path.endsWith("/")) {
            path += "/";
        }
        return path + name;
    }

    /**
     * 检查是否JAR文件
     * 
     * @param path
     * @return
     */
    public static boolean isJar(String path) {
        JarInputStream jis = null;
        try {
            jis = openJarIS(path);
            return jis.getNextJarEntry() != null;
        } catch (IOException e) {
            return false;
        } finally {
            close(jis);
        }
    }

    /**
     * 打开Jar输出流
     * 
     * @param path
     * @return
     */
    public static JarOutputStream openJarOS(String path) throws IOException {
        JarOutputStream jos = null;
        try {
            jos = new JarOutputStream(new FileOutputStream(path));
        } catch (IOException e) {
            jos = new JarOutputStream(
                    new URL(path).openConnection().getOutputStream());
        }
        return jos;
    }

    /**
     * 打开Jar输入流
     * 
     * @param path
     * @return
     * @throws IOException
     */
    public static JarInputStream openJarIS(String path) throws IOException {
        JarInputStream jis = null;
        try {
            jis = new JarInputStream(new FileInputStream(path));
        } catch (IOException e) {
            jis = new JarInputStream(new URL(path).openStream());
        }
        return jis;
    }

    /**
     * 搜索文件
     * 
     * @param jis
     * @param name
     * @return
     * @throws IOException
     */
    public static JarEntry get(JarInputStream jis, String name)
            throws IOException {
        JarEntry je;
        while ((je = jis.getNextJarEntry()) != null) {
            if (!je.isDirectory() && je.getName().equals(name)) {
                break;
            }
        }
        return je;
    }

    /**
     * 搜索文件
     * 
     * @param jis
     * @param regex
     * @return
     * @throws IOException
     */
    public static List<JarEntry> search(JarInputStream jis, String regex)
            throws IOException {
        JarEntry je;
        List<JarEntry> entries = new ArrayList<JarEntry>();
        while ((je = jis.getNextJarEntry()) != null) {
            if (!je.isDirectory() && je.getName().matches(regex)) {
                entries.add(je);
            }
        }
        return entries;
    }

    /**
     * 获取文件（规范性过滤）
     * 
     * @param name
     * @return
     */
    public static File getFile(String name) {
        File file = new File(name);
        if (!isCanonical(file)) {
            file = null;
        }
        return file;
    }

    /**
     * 获取文件（规范性过滤）
     * 
     * @param path
     * @param name
     * @return
     */
    public static File getFile(String path, String name) {
        File file = new File(path, name);
        if (!isCanonical(file)) {
            file = null;
        }
        return file;
    }

    /**
     * 获取规范路径
     * 
     * @param path
     * @return
     */
    public static String getCanonical(String path) {
        return path == null ? null : getCanonical(new File(path));
    }

    /**
     * 获取规范路径
     * 
     * @param file
     * @return
     */
    public static String getCanonical(File file) {
        String path;
        try {
            path = file.getCanonicalPath();
        } catch (IOException e) {
            path = null;
        }
        return path;
    }

    /**
     * 判断文件规范性
     * 
     * @param path
     * @return
     */
    public static boolean isCanonical(String path) {
        return getCanonical(path) != null;
    }

    /**
     * 判断文件规范性
     * 
     * @param file
     * @return
     */
    public static boolean isCanonical(File file) {
        return getCanonical(file) != null;
    }

    /**
     * 创建文件
     * 
     * @param path
     * @return
     */
    public static File createFile(String path) {
        return createFile(getFile(path));
    }

    /**
     * 创建文件
     * 
     * @param path
     * @param name
     * @return
     */
    public static File createFile(String path, String name) {
        return createFile(getFile(path, name));
    }

    /**
     * 创建文件
     * 
     * @param file
     * @return
     */
    public static File createFile(File file) {
        File parentDir = file.getParentFile();
        if (parentDir.exists() && parentDir.isDirectory()
                || parentDir.mkdirs()) {
            try {
                if (file.exists() && file.isFile() || file.createNewFile()) {
                    return file;
                }
            } catch (IOException e) {
            }
        }
        return null;
    }

    /**
     * 删除文件
     *
     * @param file
     */
    public static void delete(File file) {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                delete(subFile);
            }
        }
        file.delete();
    }

    /**
     * 创建目录
     *
     * @param file
     * @return
     */
    public static boolean mkdirs(File file) {
        return mkdirs(file, false);
    }

    /**
     * 创建目录
     *
     * @param file
     * @param force
     * @return
     */
    public static boolean mkdirs(File file, boolean force) {
        boolean result = false;
        if (isCanonical(file)) {
            if (force) {
                delete(file);
            }
            result = file.mkdirs();
        }
        return result;
    }

    /**
     * 写文件
     * 
     * @param path
     * @param content
     * @return
     */
    public static File write(String path, String content) {
        return write(path, content, false);
    }

    /**
     * 写文件
     * 
     * @param path
     * @param content
     * @param append
     * @return
     */
    public static File write(String path, String content, boolean append) {
        return write(getFile(path), content, append);
    }

    /**
     * 写文件
     * 
     * @param path
     * @param name
     * @param content
     * @return
     */
    public static File write(String path, String name, String content) {
        return write(getFile(path, name), content, false);
    }

    /**
     * 写文件
     * 
     * @param path
     * @param name
     * @param content
     * @param append
     * @return
     */
    public static File write(String path, String name, String content,
            boolean append) {
        return write(getFile(path, name), content, append);
    }

    /**
     * 写文件
     * 
     * @param file
     * @param content
     * @return
     */
    public static File write(File file, String content) {
        return write(file, content, false);
    }

    /**
     * 写文件
     * 
     * @param file
     * @param content
     * @param append
     * @return
     */
    public static File write(File file, String content, boolean append) {
        FileWriter writer = null;
        file = createFile(file);
        try {
            writer = new FileWriter(file, append);
            writer.write(content);
        } catch (Exception e) {
            file = null;
        } finally {
            close(writer);
        }
        return file;
    }

    /**
     * 重命名文件
     *
     * @param source
     * @param target
     * @return
     */
    public static File rename(File source, File target) {
        if (!source.exists()) {
            return null;
        }
        delete(target);
        if (!source.renameTo(target)) {
            InputStream is = null;
            try {
                target = copy(is = new FileInputStream(source), target);
            } catch (FileNotFoundException e) {
                target = null;
            } finally {
                close(is);
            }
        }
        return target;
    }

    /**
     * 拷贝文件
     * 
     * @param source
     * @param target
     * @return
     */
    public static File copy(InputStream source, String target) {
        return copy(source, createFile(target));
    }

    /**
     * 拷贝文件
     *
     * @param source
     * @param target
     * @return
     */
    public static File copy(InputStream source, File target) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(target);
            byte[] buffer = new byte[1024];
            int size;
            while ((size = source.read(buffer)) != -1) {
                os.write(buffer, 0, size);
                os.flush();
            }
        } catch (Exception e) {
            target = null;
        } finally {
            close(os);
        }
        return target;
    }

    /**
     * 读输入流
     * 
     * @param is
     * @return
     * @throws IOException
     */
    public static byte[] read(InputStream is) throws IOException {
        ByteArrayOutputStream bs = null;
        byte[] buffer = new byte[1024];
        try {
            bs = new ByteArrayOutputStream(1024);
            int size;
            while ((size = is.read(buffer)) != -1) {
                bs.write(buffer, 0, size);
                bs.flush();
            }
        } finally {
            close(bs);
        }
        return bs.toByteArray();
    }

    /**
     * 字节码转换
     *
     * @param instance
     * @return
     * @throws IOException
     */
    public static byte[] toBytes(Object instance) throws IOException {
        byte[] result = null;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(buffer);
            os.writeObject(instance);
            result = buffer.toByteArray();
        } finally {
            close(os);
            close(buffer);
        }
        return result;
    }

    /**
     * 关闭连接
     * 
     * @param closeable
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
            }
        }
    }
}

package net.isger.util.hitch;

import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;

import net.isger.util.Helpers;
import net.isger.util.Hitchers;
import net.isger.util.Reflects;
import net.isger.util.anno.Ignore;

/**
 * 牵引器
 * 
 * @author issing
 */
@Ignore
public class Director {

    protected static final int UNDIRECTED = 0;

    protected static final int DIRECTING = 1;

    protected static final int SUCCESS = 2;

    protected static final int FAILURE = 3;

    /** 分隔记号 */
    public static final String TOKEN_SEPARETOR = "|";

    /** 分隔表达式 */
    private static final String REGEX_SEPARETOR = "[,;:|]";

    protected transient int directed;

    protected Director() {
        directed = UNDIRECTED;
    }

    /**
     * 牵引
     * 
     * @param source
     */
    public final void direct(Object source) {
        synchronized (this) {
            if (directed == UNDIRECTED) {
                directed = DIRECTING;
            } else {
                return;
            }
        }
        if (source == null) {
            source = this;
        }
        /* 按指示路径完成搭载 */
        StringTokenizer directPath = getTokenizer(directPath());
        while (directPath.hasMoreElements()) {
            directHitch((String) directPath.nextElement(), source);
            if (directed == FAILURE) {
                throw new IllegalStateException("Fialure to direct " + source);
            }
        }
        directInflux();
        directed = SUCCESS;
    }

    /**
     * 路径牵引
     * 
     * @return
     */
    protected String directPath() {
        URL url = Reflects.getResource(this, "./");
        return url == null ? "" : url.getFile();
    }

    /**
     * 路径牵引
     * 
     * @param key
     * @param value
     * @return
     */
    protected final String directPath(String key, String value) {
        String hitchPath = Helpers.getProperty(key, value);
        if (!(value == null || hitchPath.lastIndexOf(value) >= 0)) {
            hitchPath += TOKEN_SEPARETOR + value;
        }
        return hitchPath;
    }

    /**
     * 牵引挂接
     * 
     * @param path
     */
    private void directHitch(String path, Object source) {
        Hitcher hitcher;
        try {
            hitcher = Hitchers.getHitcher(path);
        } catch (Exception e) {
            return;
        }
        if (hitcher.hitch(source)) {
            directAttach(path);
        } else {
            directed = FAILURE;
        }
    }

    /**
     * 牵引附加
     * 
     * @param path
     */
    protected void directAttach(String path) {
    }

    /**
     * 牵引汇集
     */
    protected void directInflux() {
    }

    /**
     * 路径分解器
     * 
     * @param path
     * @return
     */
    protected StringTokenizer getTokenizer(String path) {
        return new StringTokenizer(path.replaceAll(REGEX_SEPARETOR, TOKEN_SEPARETOR), TOKEN_SEPARETOR);
    }

    /**
     * 定制属性分解器
     * 
     * @param props
     * @param key
     * @return
     */
    protected StringTokenizer getTokenizer(Properties props, String key) {
        return getTokenizer(props, key, "");
    }

    /**
     * 定制属性分解器
     * 
     * @param props
     * @param key
     * @param value
     * @return
     */
    protected StringTokenizer getTokenizer(Properties props, String key, String value) {
        return getTokenizer(props.getProperty(key, value));
    }

    /**
     * 系统属性分解器
     * 
     * @param key
     * @return
     */
    protected StringTokenizer getSystemTokenizer(String key) {
        return getTokenizer(Helpers.getProperty(key));
    }

}

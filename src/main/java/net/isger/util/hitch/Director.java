package net.isger.util.hitch;

import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;

import net.isger.util.Helpers;
import net.isger.util.Hitchers;
import net.isger.util.Reflects;
import net.isger.util.anno.Ignore;

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
     * 指示
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
     * 路径
     * 
     * @return
     */
    protected String directPath() {
        URL url = Reflects.getResource(this, "./");
        return url == null ? "" : url.getFile();
    }

    /**
     * 路径
     * 
     * @param key
     * @param value
     * @return
     */
    protected final String directPath(String key, String value) {
        String hitchPath = Helpers.getProperty(key, value);
        if (!(value == null || hitchPath.endsWith(value))) {
            hitchPath += TOKEN_SEPARETOR + value;
        }
        return hitchPath;
    }

    /**
     * 搭载
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
     * 附加
     * 
     * @param path
     */
    protected void directAttach(String path) {
    }

    /**
     * 汇集
     * 
     */
    protected void directInflux() {
    }

    /**
     * 分解器
     * 
     * @param path
     * @return
     */
    protected StringTokenizer getTokenizer(String path) {
        return new StringTokenizer(
                path.replaceAll(REGEX_SEPARETOR, TOKEN_SEPARETOR),
                TOKEN_SEPARETOR);
    }

    protected StringTokenizer getTokenizer(Properties props, String key) {
        return getTokenizer(props, key, "");
    }

    protected StringTokenizer getTokenizer(Properties props, String key,
            String value) {
        return getTokenizer(props.getProperty(key, value));
    }

    protected StringTokenizer getSystemTokenizer(String key) {
        return getTokenizer(Helpers.getProperty(key));
    }

}

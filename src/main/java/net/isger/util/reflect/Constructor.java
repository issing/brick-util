package net.isger.util.reflect;

import java.util.Hashtable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.isger.util.hitch.Director;

public class Constructor {

    private static final String KEY_CONSTRUCTIONS = "brick.util.reflect.constructions";

    private static final String CONSTRUCTION_PATH = "net/isger/util/reflect/construction";

    private static final Logger LOG;

    private static final Constructor CONSTRUCTOR;

    private Map<String, Construction> constructions;

    static {
        LOG = LoggerFactory.getLogger(Constructor.class);
        CONSTRUCTOR = new Constructor();
        new Director() {
            protected String directPath() {
                return directPath(KEY_CONSTRUCTIONS, CONSTRUCTION_PATH);
            }
        }.direct(CONSTRUCTOR);
    }

    private Constructor() {
        constructions = new Hashtable<String, Construction>();
    }

    public void addConstruction(Construction construction) {
        String name = construction.getClass().getName();
        if (LOG.isDebugEnabled()) {
            LOG.info("Achieve construction [{}]", construction);
        }
        construction = constructions.put(name, construction);
        if (construction != null && LOG.isDebugEnabled()) {
            LOG.warn("(!) Discard construction [{}]", construction);
        }
    }

    /**
     * 类型检测
     * 
     * @param clazz
     * @return
     */
    public static boolean isSupport(Class<?> clazz) {
        for (Construction construction : CONSTRUCTOR.constructions.values()) {
            if (construction.isSupport(clazz)) {
                return true;
            }
        }
        return false;
    }

    public static <T> T construct(Class<? extends T> clazz, Object... args) {
        for (Construction construction : CONSTRUCTOR.constructions.values()) {
            if (construction.isSupport(clazz)) {
                try {
                    return construction.construct(clazz, args);
                } catch (Exception e) {
                }
            }
        }
        try {
            java.lang.reflect.Constructor<? extends T> cons = clazz
                    .getDeclaredConstructor();
            if (cons != null) {
                cons.setAccessible(true);
                return cons.newInstance();
            }
        } catch (Exception e) {
        }
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unsupported construct type " + clazz, e);
        }
    }
}

package net.isger.util.reflect;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.isger.util.Asserts;
import net.isger.util.Callable;
import net.isger.util.Helpers;
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
     * @param rawClass
     * @return
     */
    public static boolean isSupport(Class<?> rawClass) {
        for (Construction construction : CONSTRUCTOR.constructions.values()) {
            if (construction.isSupport(rawClass)) {
                return true;
            }
        }
        return false;
    }

    public static <T> T construct(Class<? extends T> rawClass, Object... args) {
        for (Construction construction : CONSTRUCTOR.constructions.values()) {
            if (construction.isSupport(rawClass)) {
                try {
                    return construction.construct(rawClass, args);
                } catch (Exception e) {
                }
            }
        }
        final List<Class<?>> paramTypes = new ArrayList<Class<?>>();
        Helpers.each(true, args, new Callable.Runnable() {
            public void run(Object... args) {
                paramTypes.add(args[1].getClass());
            }
        });
        try {
            java.lang.reflect.Constructor<? extends T> cons = rawClass.getDeclaredConstructor(paramTypes.toArray(new Class<?>[paramTypes.size()]));
            if (cons != null) {
                cons.setAccessible(true);
                return cons.newInstance(args);
            }
        } catch (Exception e) {
        }
        try {
            return rawClass.newInstance();
        } catch (Exception e) {
            throw Asserts.state("Unsupported construct %s", rawClass, e);
        }
    }
}

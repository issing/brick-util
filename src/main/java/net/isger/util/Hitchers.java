package net.isger.util;

import java.util.HashMap;
import java.util.Map;

import net.isger.util.hitch.Hitcher;

/**
 * 便车工具
 * 
 * @author issing
 * 
 */
public class Hitchers {

    private static final Map<String, Hitcher> HITCHERS;

    static {
        HITCHERS = new HashMap<String, Hitcher>();
    }

    private Hitchers() {
    }

    public static Hitcher getHitcher(String path) {
        Hitcher hitcher = HITCHERS.get(path);
        if (hitcher == null) {
            hitcher = new Hitcher(path);
            HITCHERS.put(path, hitcher);
        }
        return hitcher;
    }

}

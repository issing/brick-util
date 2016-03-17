package net.isger.util.reflect.type;

import net.isger.BrickUtilTest;

public class TestHitch {

    public static void hitch(Object source) {
        if (!(source instanceof BrickUtilTest)) {
            return;
        }
        System.out.println("this is TestHitch.");
    }

}

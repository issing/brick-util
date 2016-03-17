package net.isger.util.reflect.type;

import net.isger.util.reflect.Converter;

public class DefaultHitch {

    public static void hitch(Object source) {
        if (!(source instanceof Converter)) {
            return;
        }
        System.out.println("this is DefaultHitch.");
    }

}

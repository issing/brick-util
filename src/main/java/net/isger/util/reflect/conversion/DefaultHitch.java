package net.isger.util.reflect.conversion;

import net.isger.util.reflect.Converter;

public class DefaultHitch {

    public static void hitch(Object source) {
        if (!(source instanceof Converter)) {
            return;
        }
        Converter.addConversion(ClassConversion.CONVERSION);
        Converter.addConversion(BooleanConversion.CONVERSION);
        Converter.addConversion(NumberConversion.CONVERSION);
        Converter.addConversion(DateConversion.CONVERSION);
    }

}

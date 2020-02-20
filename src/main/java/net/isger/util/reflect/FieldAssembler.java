package net.isger.util.reflect;

import net.isger.util.Callable;

public abstract class FieldAssembler extends Callable<Object> {

    public Object call(Object... args) {
        return null;
    }

    public abstract Object assemble(BoundField field, Object instance, Object value, Object... args);

}

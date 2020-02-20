package net.isger.util.reflect;

public interface ClassAssembler {

    public Class<?> assemble(Class<?> rawClass);

    public FieldAssembler getFieldAssembler();

}

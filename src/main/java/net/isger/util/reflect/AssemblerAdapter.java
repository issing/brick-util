package net.isger.util.reflect;

public class AssemblerAdapter extends FieldAssembler implements ClassAssembler {

    private ClassAssembler assembler;

    public AssemblerAdapter() {
    }

    public AssemblerAdapter(ClassAssembler assembler) {
        this.assembler = assembler;
    }

    public Class<?> assemble(Class<?> rawClass) {
        return assembler == null ? rawClass : assembler.assemble(rawClass);
    }

    public final FieldAssembler getFieldAssembler() {
        return this;
    }

    public Object assemble(BoundField field, Object instance, Object value, Object... args) {
        return assembler == null || assembler.getFieldAssembler() == null ? value : assembler.getFieldAssembler().assemble(field, instance, value, args);
    }

}

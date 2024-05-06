package net.isger.util.reflect;

import java.util.Map;

public class AssemblerAdapter extends FieldAssembler implements ClassAssembler {

    private ClassAssembler assembler;

    public AssemblerAdapter() {
    }

    public AssemblerAdapter(ClassAssembler assembler) {
        this.assembler = assembler;
    }

    public Class<?> assemble(Class<?> rawClass) {
        return this.assembler == null ? rawClass : this.assembler.assemble(rawClass);
    }

    public final FieldAssembler getFieldAssembler() {
        return this;
    }

    public Object assemble(BoundField field, Object instance, Object value, @SuppressWarnings("unchecked") Map<String, ? extends Object>... args) {
        return this.assembler == null || this.assembler.getFieldAssembler() == null ? value : this.assembler.getFieldAssembler().assemble(field, instance, value, args);
    }

}

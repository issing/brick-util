package net.isger.util.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.isger.brick.blue.ClassSeal;
import net.isger.brick.blue.Marks.ACCESS;
import net.isger.brick.blue.Marks.MISC;
import net.isger.brick.blue.Marks.OPCODES;
import net.isger.brick.blue.Marks.TYPE;
import net.isger.brick.blue.Marks.VERSION;
import net.isger.brick.blue.MethodSeal;

public class Standin extends ClassLoader {

    private static final String CLASS_STANDIN = "Standin";

    private static final String FIELD_STANDIN = "Brick$util$reflect$standin";

    private Object source;

    public Standin(Class<?> clazz) {
        ClassSeal cs;
        Constructor<?>[] constructors;
        if (clazz.isInterface()) {
            cs = ClassSeal.create(VERSION.V0104.value, ACCESS.PUBLIC.value,
                    CLASS_STANDIN, TYPE.OBJECT.name, clazz.getName());
            constructors = Object.class.getDeclaredConstructors();
        } else {
            cs = ClassSeal.create(VERSION.V0104.value, ACCESS.PUBLIC.value,
                    CLASS_STANDIN, clazz.getName());
            constructors = clazz.getDeclaredConstructors();
        }
        makeFields(cs);
        makeConstructors(cs, constructors);
        makeMethods(cs, clazz.getMethods());
        byte[] code = net.isger.brick.blue.Compiler.compile(cs);
        try {
            source = this.defineClass(CLASS_STANDIN, code, 0, code.length)
                    .newInstance();
            Field field = source.getClass().getField(FIELD_STANDIN);
            field.set(source, this);
        } catch (Exception e) {
            throw new IllegalStateException("Failure create stand-in for "
                    + clazz);
        }
    }

    private void makeFields(ClassSeal cs) {
        cs.makeField(ACCESS.PUBLIC.value, Standin.class.getName(),
                FIELD_STANDIN);
    }

    private void makeConstructors(ClassSeal cs, Constructor<?>[] constructors) {
        MethodSeal ms;
        int mod;
        String[] argTypeNames;
        for (Constructor<?> constructor : constructors) {
            mod = constructor.getModifiers();
            if (Modifier.isProtected(mod) || Modifier.isPublic(mod)) {
                argTypeNames = TYPE.getArgTypeNames(constructor
                        .getParameterTypes());
                ms = cs.makeMethod(ACCESS.PUBLIC.value, "void", "<init>",
                        argTypeNames);
                ms.markOperate("super(" + ms.hashCode() + ")", constructor
                        .getDeclaringClass().getName(),
                        OPCODES.INVOKESPECIAL.value, TYPE.VOID.name, "<init>",
                        argTypeNames);
                ms.coding("this", "super(" + ms.hashCode() + ")",
                        MISC.args(argTypeNames.length));
            }
        }
    }

    private void makeMethods(ClassSeal cs, Method[] methods) {
        MethodSeal ms;
        int mod;
        String[] argTypeNames;
        for (Method method : methods) {
            mod = method.getModifiers();
            if ((Modifier.isProtected(mod) || Modifier.isPublic(mod))
                    && !(Modifier.isFinal(mod) || Modifier.isStatic(mod))) {
                argTypeNames = TYPE.getArgTypeNames(method.getParameterTypes());
                ms = cs.makeMethod(ACCESS.PUBLIC.value, method.getReturnType()
                        .getName(), method.getName(), argTypeNames);
                if (Modifier.isAbstract(mod)) {
                    ms.markOperate("action()", Standin.class.getName(),
                            OPCODES.INVOKEVIRTUAL.value, TYPE.VOID.name,
                            "action");
                    ms.coding(FIELD_STANDIN, "action()");
                } else {
                    ms.markOperate("super.method()", method.getDeclaringClass()
                            .getName(), OPCODES.INVOKESPECIAL.value, method
                            .getReturnType().getName(), method.getName(),
                            argTypeNames);
                    ms.coding("this", "super.method()",
                            MISC.args(argTypeNames.length));
                }
            }
        }
    }

    public Object getSource() {
        return this.source;
    }

    public void action() {
        System.out.println("this is action.");
    }

}

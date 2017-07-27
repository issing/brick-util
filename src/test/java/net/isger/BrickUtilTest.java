package net.isger;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.isger.util.Hitchers;
import net.isger.util.Reflects;
import net.isger.util.hitch.Hitcher;
import net.isger.util.reflect.Standin;

public class BrickUtilTest extends TestCase {

    public BrickUtilTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(BrickUtilTest.class);
    }

    public void testUtil() {
        Hitcher hitcher = Hitchers.getHitcher("net/isger/util/reflect/type");
        System.out.println(hitcher.hitch(this));
        // System.out.println(Void.TYPE.getName());
        TestBean testBean = new Standin<TestBean>(TestBean.class) {
            public Object action(Method method, Object[] args) {
                System.out.println(method);
                for (Object arg : args) {
                    System.out.println(arg);
                }
                return null;
            }
        }.getSource();
        testBean.test("test.name", "test.password");
        assertTrue(true);

        Map<String, Object> values = new HashMap<>();
        values.put("value", "a");
        values.put("a.value", "a.a");
        System.out.println(Reflects.newInstance(A.class, values).a.value);
    }

    public static interface TestBean {

        public void test(String accounts, String password);

    }

    public static class A {

        private String value;

        private A a;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public A getA() {
            return a;
        }

        public void setA(A a) {
            this.a = a;
        }

    }

}

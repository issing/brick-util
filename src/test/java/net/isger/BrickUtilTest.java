package net.isger;

import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.isger.util.Hitchers;
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
    }

    public static interface TestBean {

        public void test(String accounts, String password);

    }
}

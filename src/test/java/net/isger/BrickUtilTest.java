package net.isger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.isger.util.Hitchers;
import net.isger.util.hitch.Hitcher;

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
        // TestBean testBean = (TestBean) new
        // Standin(TestBean.class).getSource();
        // testBean.test();
        // System.out.println(Object.class.isAssignableFrom(Integer.TYPE));
        // Asserts.isInstanceOf(String.class, 1, "what");
        assertTrue(true);
    }

    public static interface TestBean {

        public void test();

    }
}

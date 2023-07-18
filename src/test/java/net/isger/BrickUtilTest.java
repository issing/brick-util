package net.isger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("value", "a");
        values.put("a.value", "a.a");
        System.out.println(Reflects.newInstance(A.class, values).a.value);
    }

    public static void testSql() {
        String sql = "SELECT * FROM table1; INSERT INTO table2 VALUES ('value1'''';'';;'';;;'''''''';''value2'); UPDATE table3 SET column1 = 'value;3';";
        List<String> statements = parseSQLStatements(sql);
        for (String statement : statements) {
            System.out.println(statement);
        }
    }

    public static List<String> parseSQLStatements(String sql) {
        List<String> statements = new ArrayList<>();

        char[] chars = sql.toCharArray();
        StringBuilder sb = new StringBuilder();
        boolean insideQuotes = false; // 是否在引号内部
        boolean insideEscapedQuotes = false; // 是否在转义引号内部

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (insideQuotes) {
                sb.append(c);
                if (c == '\'' && !insideEscapedQuotes) {
                    insideQuotes = false;
                } else if (c == '\'' && insideEscapedQuotes) {
                    insideEscapedQuotes = false;
                } else if (c == '\\' && i + 1 < chars.length && chars[i + 1] == '\'') {
                    insideEscapedQuotes = true;
                }
            } else {
                sb.append(c);
                if (c == ';') {
                    String statement = sb.toString().trim();
                    if (!statement.isEmpty()) {
                        statements.add(statement);
                    }
                    sb.setLength(0); // 清空StringBuilder
                } else if (c == '\'' && i + 1 < chars.length && chars[i + 1] != '\'') {
                    insideQuotes = true;
                }
            }
        }

        // 添加最后一个语句
        String lastStatement = sb.toString().trim();
        if (!lastStatement.isEmpty()) {
            statements.add(lastStatement);
        }

        return statements;
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

package net.isger.util.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.isger.util.Helpers;
import net.isger.util.Strings;

/**
 * SQL记录
 * 
 * @author issing
 *
 */
public class SqlEntry implements Iterable<SqlEntry> {

    protected String sql;

    protected Object[] values;

    protected List<SqlEntry> entries;

    public SqlEntry(String sql, Object... values) {
        this.sql = sql;
        this.values = Helpers.coalesce(values, new Object[1]);
        this.entries = parse(this.sql, this.values);
    }

    private SqlEntry(String sql, Object[] values, int index, int count) {
        this.sql = sql;
        int size = values.length;
        this.values = values instanceof Object[][] ? new Object[size][count] : new Object[count];
        if (values instanceof Object[][]) {
            for (int i = 0; i < size; i++) {
                System.arraycopy(values[i], index, this.values[i], 0, count);
            }
        } else {
            System.arraycopy(values, index, this.values, 0, count);
        }
        this.entries = new ArrayList<SqlEntry>();
        this.entries.add(this);
    }

    public Iterator<SqlEntry> iterator() {
        return entries.iterator();
    }

    public String getSql() {
        return sql;
    }

    public String getSql(SqlEntry entry) {
        return entry.sql;
    }

    public Object[] getValues() {
        return values;
    }

    public Object[] getValues(SqlEntry entry) {
        return entry.values;
    }

    public List<SqlEntry> getEntries() {
        return Collections.unmodifiableList(this.entries);
    }

    public void wrap(SqlEntry entry) {
        this.sql = entry.sql;
        this.values = entry.values;
        this.entries = parse(this.sql, this.values);
    }

    protected List<SqlEntry> parse(String sql, Object[] values) {
        List<SqlEntry> statements = new ArrayList<SqlEntry>();
        char[] chars = sql.trim().toCharArray();
        StringBuilder buffer = new StringBuilder();
        int index = 0;
        int count = 0;
        boolean insideQuotes = false; // 是否在引号内部
        boolean insideEscapedQuotes = false; // 是否在转义引号内部
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (insideQuotes) {
                buffer.append(c);
                if (c == '\'' && !insideEscapedQuotes) {
                    insideQuotes = false;
                } else if (c == '\'' && insideEscapedQuotes) {
                    insideEscapedQuotes = false;
                } else if (c == '\\' && i + 1 < chars.length && chars[i + 1] == '\'') {
                    insideEscapedQuotes = true;
                }
            } else {
                buffer.append(c);
                if (c == ';') {
                    String statement = buffer.toString().trim();
                    if (statement.length() > 0) {
                        count = Strings.count(statement, "?");
                        statements.add(new SqlEntry(statement, values, index, count));
                        index += count;
                    }
                    buffer.setLength(0); // 清空StringBuilder
                } else if (c == '\'' && i + 1 < chars.length && chars[i + 1] != '\'') {
                    insideQuotes = true;
                }
            }
        }
        // 添加最后一个语句
        String statement = buffer.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(new SqlEntry(statement, values, index, Strings.count(statement, "?")));
        }
        return statements;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer(sql.length() * 2);
        buffer.append(sql);
        if (values != null && values.length > 0) {
            buffer.append("[").append(values[0]);
            for (int i = 1; i < values.length; i++) {
                buffer.append(", ").append(values[1]);
            }
            buffer.append("]");
        }
        return buffer.toString();
    }

}
package net.isger.util.sql;

/**
 * SQL记录
 * 
 * @author issing
 *
 */
public class SqlEntry {

    protected final String sql;

    protected final Object[] values;

    public SqlEntry(String sql, Object... values) {
        this.sql = sql;
        this.values = values;
    }

    public String getSql() {
        return sql.toString();
    }

    public Object[] getValues() {
        return values;
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
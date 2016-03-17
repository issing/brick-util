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

}
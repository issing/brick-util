package net.isger.util.sql;

public class PageSql extends SqlEntry {

    private Page page;

    public PageSql(Page page, String sql, Object... values) {
        super(sql, values);
        this.page = page;
    }

    public Page getPage() {
        return page;
    }

    public final String getSql() {
        return getWrapSql(sql);
    }

    public final Object[] getValues() {
        return getWrapValues(values);
    }

    public String getOriginSql() {
        return sql;
    }

    public Object[] getOriginValues() {
        return this.values;
    }

    public String getCountSql() {
        if (page.getTotal() > 0) {
            return null;
        }
        return "select count(1) from (" + super.getSql() + ") t";
    }

    public String getWrapSql(String sql) {
        return sql + " limit ?, ?";
    }

    public Object[] getWrapValues(Object[] values) {
        int valCount = 2;
        Object[] wrapValues = null;
        if (values != null) {
            valCount += values.length;
            wrapValues = new Object[valCount];
            System.arraycopy(values, 0, wrapValues, 0, values.length);
        } else {
            wrapValues = new Object[valCount];
        }
        wrapValues[valCount - 1] = page.getLimit();
        wrapValues[valCount - 2] = (page.getStart() - 1) * page.getLimit();
        return wrapValues;
    }

    public void wrap(SqlEntry entry) {
        this.sql = entry.sql;
        this.values = entry.values;
    }

}

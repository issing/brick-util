package net.isger.util.sql;

public class PageSql extends SqlEntry {

    private Pager page;

    public PageSql(Pager page, String sql, Object... values) {
        super(sql, values);
        this.page = page;
    }

    public Pager getPage() {
        return page;
    }

    public final String getSql() {
        return getWrapSql(sql);
    }

    public final String getSql(SqlEntry entry) {
        return getWrapSql(entry.sql);
    }

    public final Object[] getValues() {
        return getWrapValues(values);
    }

    public final Object[] getValues(SqlEntry entry) {
        return getWrapValues(entry.values);
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
        SqlEntry lastEntry = this.entries.get(this.entries.size() - 1);
        return "SELECT COUNT(1) FROM (" + lastEntry.getSql() + ") t";
    }

    public Object[] getCountValues() {
        SqlEntry lastEntry = this.entries.get(this.entries.size() - 1);
        return lastEntry.getValues();
    }

    public String getWrapSql(String sql) {
        return sql + " LIMIT ?, ?";
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

}

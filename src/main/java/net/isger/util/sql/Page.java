package net.isger.util.sql;

public class Page {

    private int start;

    private int limit;

    private int total;

    public Page() {
        this(1);
    }

    public Page(int start) {
        this(start, 10);
    }

    public Page(int start, int limit) {
        this.setStart(start);
        this.setLimit(limit);
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        if (start < 1) {
            start = 1;
        }
        this.start = start;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if (limit < 1) {
            limit = 1;
        }
        this.limit = limit;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCount() {
        return total / limit + ((total % limit) > 0 ? 1 : 0);
    }

}

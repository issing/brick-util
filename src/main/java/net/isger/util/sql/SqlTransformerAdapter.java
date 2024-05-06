package net.isger.util.sql;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.isger.util.anno.Ignore;
import net.isger.util.anno.Ignore.Mode;

@Ignore
public class SqlTransformerAdapter implements SqlTransformer {

    @Ignore(mode = Mode.INCLUDE)
    private Map<String, Object> parameters;

    public SqlTransformerAdapter() {
        this.parameters = new HashMap<String, Object>();
    }

    public boolean hasReady() {
        return true;
    }

    public Status getStatus() {
        return Status.STATELESS;
    }

    public void initial() {
    }

    protected final Object getParameter(String name) {
        return this.parameters.get(name);
    }

    protected final Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(this.parameters);
    }

    public SqlEntry transform(SqlEntry entry) {
        return entry;
    }

    public SqlEntry transform(String sql, Object value) {
        if (value instanceof Object[]) {
            return new SqlEntry(sql, (Object[]) value);
        }
        return new SqlEntry(sql, value);
    }

    public void destroy() {
        this.parameters.clear();
    }

}

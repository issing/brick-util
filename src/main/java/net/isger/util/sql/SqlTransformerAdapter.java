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
        parameters = new HashMap<String, Object>();
    }

    public void initial() {
    }

    protected final Object getParameter(String name) {
        return parameters.get(name);
    }

    protected final Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
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
        parameters.clear();
    }

}

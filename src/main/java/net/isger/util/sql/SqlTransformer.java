package net.isger.util.sql;

import net.isger.util.Manageable;

public interface SqlTransformer extends Manageable {

    public SqlEntry transform(SqlEntry entry);

    public SqlEntry transform(String sql, Object value);

}

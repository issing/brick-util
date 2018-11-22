package net.isger.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import net.isger.util.anno.Alias;
import net.isger.util.reflect.BoundField;
import net.isger.util.sql.SqlEntry;

/**
 * 标准SQL工具
 * 
 * @author issing
 * 
 */
public class Sqls {

    private static final Logger LOG;

    /** SQL配置集合 */
    private final static Map<String, Properties> CACHE_SQLS;

    private static final Gson GSON;

    static {
        LOG = LoggerFactory.getLogger(Sqls.class);
        CACHE_SQLS = new HashMap<String, Properties>();
        GSON = new Gson();
    }

    /**
     * 转换为Java约定字段命名
     * 
     * @param columnName
     * @return
     */
    public static String toFieldName(String columnName) {
        char[] chs = columnName.toLowerCase().toCharArray();
        StringBuffer fieldName = new StringBuffer(chs.length);
        boolean hasUpper = false;
        for (char ch : chs) {
            // 跳过“_”符号，并设置接下来其它字符为大写
            if (ch == '_') {
                hasUpper = true;
                continue;
            } else if (hasUpper) {
                ch = Character.toUpperCase(ch);
                hasUpper = false; // 重置大写状态
            }
            fieldName.append(ch);
        }
        return fieldName.toString();
    }

    /**
     * 转换为SQL约定列命名
     * 
     * @param fieldName
     * @return
     */
    public static String toColumnName(String fieldName) {
        // 去除所有“_”符号
        char[] chs = fieldName.replaceAll("[_]", "").toCharArray();
        StringBuffer columnName = new StringBuffer(chs.length + 16);
        for (char ch : chs) {
            // 遇大写字母前加“_”符号
            if (Character.isUpperCase(ch)) {
                columnName.append('_');
            }
            columnName.append(Character.toLowerCase(ch));
        }
        return columnName.toString();
    }

    /**
     * 转换为SQL约定表命名
     * 
     * @param tableName
     * @return
     */
    public static String toTableName(String tableName) {
        tableName = toColumnName(tableName);
        if (!Strings.startWithIgnoreCase(tableName, "t[_]")) {
            tableName = "t_" + tableName;
        }
        return tableName;
    }

    /**
     * 获取表名
     * 
     * @param clazz
     * @return
     */
    public static String getTableName(Class<?> clazz) {
        return getTableName(clazz, null);
    }

    /**
     * 获取表名
     * 
     * @param clazz
     * @param mask
     * @return
     */
    public static String getTableName(Class<?> clazz, String mask) {
        String tableName;
        // 优先别名（无需转换名称）
        Alias table = clazz.getAnnotation(Alias.class);
        if (table != null) {
            tableName = table.value();
            if (Strings.isNotEmpty(tableName)) {
                return tableName;
            }
        }
        // 采用类名（需要转换名称）
        tableName = clazz.getSimpleName();
        if (Strings.isNotEmpty(mask)) {
            tableName = Strings.replaceIgnoreCase(tableName, mask);
        }
        return toTableName(Strings.toLower(tableName));
    }

    /**
     * 转换为约定数据结构
     * 
     * @param resultSet
     *            数据库查询结果集
     * @return <b>网格数据结构： </b><br/>
     *         第一个元素：<code>String[]</code>（列名数组）<br/>
     *         第二个元素：<code>Object[][]</code>（数据二维数组）<br/>
     *         <b>例如：</b><br/>
     *         第一个元素: <code>String[]{"column1", "column2"}</code><br/>
     *         第二个元素: <code>Object[][]{{"data1", 1}, {"data2", 2}}</code><br/>
     *         <b>图示：</b>
     * 
     *         <pre>
     *         +-----------------------+
     *         |  column1  |  column2  |
     *         +-----------------------+
     *         |   data1   |     1     |
     *         |   data2   |     2     |
     *         +-----------------------+
     *         </pre>
     */
    public static Object[] getGridData(ResultSet resultSet) {
        List<Object[]> result = new ArrayList<Object[]>();
        String[] columns = null;
        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int count = metaData.getColumnCount();
            columns = new String[count];
            for (int i = 0; i < count;) {
                columns[i] = toFieldName(getColumnName(metaData, ++i));
            }
            Object[] info = null;
            while (resultSet.next()) {
                info = new Object[count];
                for (int i = 0; i < count;) {
                    info[i] = resultSet.getObject(++i);
                }
                result.add(info);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return new Object[] { columns,
                result.toArray(new Object[result.size()][]) };
    }

    /**
     * 获取网格数据
     * 
     * @param bean
     *            实例对象
     * @return<b>网格数据结构： </b><br/>
     *                   第一个元素：<code>String[]</code>（列名数组）<br/>
     *                   第二个元素：<code>Object[]</code>（数据一维数组）<br/>
     *                   <b>例如：</b><br/>
     *                   第一个元素: <code>String[]{"column1", "column2"}</code><br/>
     *                   第二个元素: <code>Object[]{"data1", 1}</code><br/>
     *                   <b>图示：</b>
     * 
     *                   <pre>
     *         +-----------------------+
     *         |  column1  |  column2  |
     *         +-----------------------+
     *         |   data1   |     1     |
     *         +-----------------------+
     *                   </pre>
     */
    public static Object[] getGridData(Object bean) {
        String column;
        Object value;
        List<String> columns = new ArrayList<String>();
        List<Object> row = new ArrayList<Object>();
        BoundField field;
        for (List<BoundField> fields : Reflects.getBoundFields(bean.getClass())
                .values()) {
            field = fields.get(0);
            column = Strings.empty(field.getAlias(),
                    Sqls.toColumnName(field.getName()));
            value = field.getValue(bean);
            if (value != null) {
                columns.add(column);
                row.add(value);
            }
        }
        return new Object[] { columns.toArray(new String[columns.size()]),
                row.toArray() };
    }

    /**
     * 获取列名
     * 
     * @param metaData
     * @param index
     * @return
     * @throws SQLException
     */
    private static String getColumnName(ResultSetMetaData metaData, int index)
            throws SQLException {
        String name = metaData.getColumnName(index);
        if (Strings.isEmpty(name)) {
            name = metaData.getColumnLabel(index);
        }
        return name;
    }

    /**
     * 修改数据
     * 
     * @param entry
     * @param conn
     * @return
     * @throws RuntimeException
     */
    public static Object modify(SqlEntry entry, Connection conn)
            throws RuntimeException {
        String sql = entry.getSql();
        Object values = entry.getValues();
        if (values instanceof Object[][]) {
            return modify(sql, (Object[][]) values, conn);
        }
        return modify(sql, (Object[]) values, conn);
    }

    /**
     * 修改数据（批量）
     * 
     * @param clazz
     * @param id
     * @param values
     * @param conn
     * @param args
     * @return
     */
    public static int[] modify(Class<?> clazz, String id, Object[][] values,
            Connection conn, Object... args) {
        return modify(clazz, null, id, values, conn, args);
    }

    public static int[] modify(Class<?> clazz, String dialectName, String id,
            Object[][] values, Connection conn, Object... args) {
        return modify(getSQL(clazz, dialectName, id, args), values, conn);
    }

    /**
     * 修改数据（批量）
     * 
     * @param sql
     * @param values
     * @param conn
     * @return
     */
    public static int[] modify(String sql, Object[][] values, Connection conn) {
        PreparedStatement stat = getStatement(sql, values, conn);
        try {
            return stat.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            close(stat);
        }
    }

    /**
     * 修改数据
     * 
     * @param clazz
     * @param id
     * @param values
     * @param conn
     * @param args
     * @return
     */
    public static int modify(Class<?> clazz, String id, Object[] values,
            Connection conn, Object... args) {
        return modify(getSQL(clazz, id, args), values, conn);
    }

    /**
     * 修改数据
     * 
     * @param sql
     * @param conn
     * @return
     */
    public static int modify(String sql, Connection conn) {
        return modify(sql, (Object[]) null, conn);
    }

    /**
     * 修改数据
     * 
     * @param sql
     * @param values
     * @param conn
     * @return
     */
    public static int modify(String sql, Object[] values, Connection conn) {
        PreparedStatement stat = getStatement(sql, values, conn);
        try {
            return stat.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            close(stat);
        }
    }

    /**
     * 查询数据
     * 
     * @param entry
     * @param conn
     * @return
     * @throws RuntimeException
     */
    public static Object[] query(SqlEntry entry, Connection conn)
            throws RuntimeException {
        return query(entry.getSql(), entry.getValues(), conn);
    }

    /**
     * 查询数据
     * 
     * @param clazz
     * @param id
     * @param values
     * @param conn
     * @param args
     * @return
     */
    public static Object[] query(Class<?> clazz, String id, Object[] values,
            Connection conn, Object... args) {
        return query(clazz, null, id, values, conn, args);
    }

    public static Object[] query(Class<?> clazz, String dialectName, String id,
            Object[] values, Connection conn, Object... args) {
        return query(getSQL(clazz, dialectName, id, args), values, conn);
    }

    /**
     * 查询数据
     * 
     * @param sql
     * @param conn
     * @return
     */
    public static Object[] query(String sql, Connection conn) {
        return query(sql, null, conn);
    }

    /**
     * 查询数据
     * 
     * @param sql
     * @param values
     * @param conn
     * @return
     */
    public static Object[] query(String sql, Object[] values, Connection conn) {
        PreparedStatement stat = getStatement(sql, values, conn);
        ResultSet resultSet = null;
        try {
            resultSet = stat.executeQuery();
            return getGridData(resultSet);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            close(resultSet);
            close(stat);
        }
    }

    /**
     * 获取语句声明
     * 
     * @param sql
     * @param values
     * @param conn
     * @return
     */
    private static PreparedStatement getStatement(String sql, Object[] values,
            Connection conn) {
        if (LOG.isDebugEnabled()) {
            LOG.info("Preparing statement: {}", sql);
        }
        try {
            return prepare(conn.prepareStatement(sql), values);
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e.getCause());
        }
    }

    /**
     * 获取语句声明（批量）
     * 
     * @param sql
     * @param values
     * @param conn
     * @return
     */
    private static PreparedStatement getStatement(String sql, Object[][] values,
            Connection conn) {
        if (LOG.isDebugEnabled()) {
            LOG.info("Preparing batch statement: {}", sql);
        }
        try {
            PreparedStatement stat = conn.prepareStatement(sql);
            if (values != null) {
                for (Object[] batch : values) {
                    prepare(stat, batch).addBatch();
                }
            }
            return stat;
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e.getCause());
        }
    }

    /**
     * 预处理语句
     * 
     * @param stat
     * @param values
     * @return
     * @throws SQLException
     */
    private static PreparedStatement prepare(PreparedStatement stat,
            Object[] values) throws SQLException {
        Object value;
        int size = values == null ? 0 : values.length;
        try {
            size = Math.min(size,
                    stat.getParameterMetaData().getParameterCount());
        } catch (Exception e) {
        }
        if (LOG.isDebugEnabled() && size > 0) {
            StringBuffer format = new StringBuffer(20 + 4 * size);
            format.append("Preparing parameter: [{}");
            for (int i = 1; i < size; i++) {
                format.append(", {}");
            }
            format.append("]");
            LOG.info(format.toString(), values);
        }
        int amount = 0;
        while (amount < size) {
            if ((value = values[amount++]) instanceof Date) {
                stat.setObject(amount, new Timestamp(((Date) value).getTime()));
            } else if (value instanceof Number || value instanceof Boolean
                    || value instanceof String) {
                stat.setObject(amount, value);
            } else {
                stat.setObject(amount, GSON.toJson(value));
            }
        }
        return stat;
    }

    /**
     * 关闭结果集
     * 
     * @param resultSet
     */
    public static void close(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
            }
        }
    }

    /**
     * 关闭语句
     * 
     * @param stat
     */
    public static void close(Statement stat) {
        if (stat != null) {
            try {
                stat.close();
            } catch (SQLException e) {
            }
        }
    }

    /**
     * 关闭连接
     * 
     * @param conn
     */
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
            }
        }
    }

    /**
     * 获取语句
     * 
     * @param clazz
     * @param id
     * @param args
     * @return
     */
    public static String getSQL(Class<?> clazz, String id, Object... args) {
        return getSQL(clazz, null, id, args);
    }

    /**
     * 获取语句
     * 
     * @param clazz
     * @param dialectName
     * @param id
     * @param args
     * @return
     */
    public static String getSQL(Class<?> clazz, String dialectName, String id,
            Object... args) {
        String name = clazz.getName().replaceAll("[.]", "/");
        /* 缓存配置 */
        Properties sqls = CACHE_SQLS.get(name);
        if (sqls == null) {
            sqls = Helpers.getProperties(clazz, name + ".sql");
            /* 方言配置 */
            if (Strings.isNotEmpty(dialectName)) {
                sqls = Helpers.load(sqls, clazz,
                        name + "$" + dialectName.trim().toLowerCase() + ".sql");
            }
            // 配置文件中必须包含配置语句
            Asserts.throwState(sqls != null, "Not found the [%s.sql.xml] file",
                    name);
            CACHE_SQLS.put(name, sqls);
        }
        /* 配置语句 */
        String sql = sqls.getProperty(id);
        Asserts.throwState(Strings.isNotEmpty(sql),
                "Not found the sql [%s] in the [%s.sql.xml] file", id, name);
        return Strings.format(sql, args);
    }
}

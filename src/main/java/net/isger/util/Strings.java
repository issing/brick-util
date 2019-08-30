package net.isger.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * 字符串工具
 * 
 * @author issing
 *
 */
public class Strings {

    private Strings() {
    }

    /**
     * 去头尾空串
     *
     * @param value
     * @return
     */
    public static String trim(Object value) {
        if (value instanceof String) {
            return ((String) value).replaceFirst("^[\\s ]+", "").replaceFirst("[\\s ]+$", "");
        } else {
            return Strings.empty(value);
        }
    }

    /**
     * 空字符串
     * 
     * @param value
     * @return
     */
    public static boolean isEmpty(Object value) {
        return value == null || value.toString().matches("^[\\s ]*$");
    }

    /**
     * 非空字符串
     * 
     * @param value
     * @return
     */
    public static boolean isNotEmpty(Object value) {
        return !isEmpty(value);
    }

    /**
     * 空替换操作
     * 
     * @param value
     * @return
     */
    public static String empty(Object value) {
        return empty(value, "");
    }

    /**
     * 空替换操作
     * 
     * @param value
     * @param def
     * @return
     */
    public static String empty(Object value, String def) {
        return isEmpty(value) ? def : trim(value.toString());
    }

    /**
     * 忽略大小写匹配
     * 
     * @param value
     * @param regex
     * @return
     */
    public static boolean matchsIgnoreCase(String value, String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(value).matches();
    }

    /**
     * 忽略大小写包含
     * 
     * @param value
     * @param regex
     * @return
     */
    public static boolean containsIgnoreCase(String value, String regex) {
        return matchsIgnoreCase(value, "^.*(" + regex + ").*$");
    }

    /**
     * 字符串起始匹配
     * 
     * @param value
     * @param regex
     * @return
     */
    public static boolean startWithIgnoreCase(String value, String regex) {
        return matchsIgnoreCase(value, "^(" + regex + ").*$");
    }

    /**
     * 字符串结尾匹配
     * 
     * @param value
     * @param regex
     * @return
     */
    public static boolean endWithIgnoreCase(String value, String regex) {
        return matchsIgnoreCase(value, "^.*(" + regex + ")$");
    }

    /**
     * 数据内容判断
     * 
     * @param source
     * @param target
     * @return
     */
    public static boolean equals(byte[] source, byte[] target) {
        return new String(source).equals(new String(target));
    }

    public static boolean equalsIgnoreCase(String source, Object... targets) {
        if (targets != null) {
            for (Object target : targets) {
                if (target == null) {
                    continue;
                }
                if (source.equalsIgnoreCase(String.valueOf(target))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 忽略大小写替换
     * 
     * @param value
     * @param regex
     * @return
     */
    public static String replaceIgnoreCase(String value, String regex) {
        return replaceIgnoreCase(value, regex, "");
    }

    /**
     * 忽略大小写替换
     * 
     * @param value
     * @param regex
     * @param content
     * @return
     */
    public static String replaceIgnoreCase(String value, String regex, String content) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(value).replaceAll(content);
    }

    /**
     * 转换编码
     * 
     * @param data
     * @param sourceCharset
     * @param targetCharset
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String toCharset(byte[] data, String sourceCharset, String targetCharset) throws UnsupportedEncodingException {
        if (isEmpty(sourceCharset) || sourceCharset.equals(targetCharset)) {
            return new String(data, targetCharset);
        }
        return new String(Charset.forName(targetCharset).encode(Charset.forName(sourceCharset).decode(ByteBuffer.wrap(data))).array(), targetCharset);
    }

    /**
     * 首字母大写
     * 
     * @param value
     * @return
     */
    public static String toUpper(String value) {
        int len = value.length();
        if (len > 0) {
            char[] cs = value.toCharArray();
            cs[0] = Character.toUpperCase(cs[0]);
            value = new String(cs);
        }
        return value;
    }

    /**
     * 首字母小写
     * 
     * @param value
     * @return
     */
    public static String toLower(String value) {
        int len = value.length();
        if (len > 0) {
            char[] cs = value.toCharArray();
            cs[0] = Character.toLowerCase(cs[0]);
            value = new String(cs);
        }
        return value;
    }

    /**
     * 遍历操作
     * 
     * @param value
     * @param token
     * @param callable
     * @return
     */
    public static Object[] each(String value, String token, Callable<Object> callable) {
        return each(new StringTokenizer(value, token), callable);
    }

    /**
     * 遍历操作
     * 
     * @param tokenizer
     * @param callable
     * @return
     */
    public static Object[] each(StringTokenizer tokenizer, Callable<Object> callable) {
        List<Object> result = new ArrayList<Object>();
        while (tokenizer.hasMoreTokens()) {
            result.add(callable.call(tokenizer.nextToken()));
        }
        return result.toArray();
    }

    /**
     * 格式化操作
     * 
     * @param message
     * @param args
     * @return
     */
    public static String format(String message, Object... args) {
        try {
            return String.format(message, args);
        } catch (Exception e) {
            return message;
        }
    }

    /**
     * 追加
     *
     * @param values
     * @return
     */
    public static String join(Collection<?> values) {
        return join(false, values);
    }

    /**
     * 追加
     *
     * @param isCompact
     * @param values
     * @return
     */
    public static String join(boolean isCompact, Collection<?> values) {
        return join(isCompact, null, values);
    }

    /**
     * 追加
     *
     * @param isCompact
     * @param separator
     * @param values
     * @return
     */
    public static String join(boolean isCompact, String separator, Collection<?> values) {
        return join(isCompact, separator, null, values);
    }

    /**
     * 追加
     *
     * @param isCompact
     * @param separator
     * @param seal
     * @param values
     * @return
     */
    public static String join(boolean isCompact, String separator, String seal, Collection<?> values) {
        return join(isCompact, separator, seal, seal, values);
    }

    /**
     * 追加
     *
     * @param isCompact
     * @param separator
     * @param beginSeal
     * @param endSeal
     * @param values
     * @return
     */
    public static String join(boolean isCompact, String separator, String beginSeal, String endSeal, Collection<?> values) {
        return join(isCompact, separator, beginSeal, endSeal, values == null ? null : values.toArray());
    }

    /**
     * 追加
     * 
     * @param values
     * @return
     */
    public static String join(Object[] values) {
        return join(false, values);
    }

    /**
     * 追加
     *
     * @param isCompact
     * @param values
     * @return
     */
    public static String join(boolean isCompact, Object[] values) {
        return join(isCompact, values, 0);
    }

    /**
     * 追加
     *
     * @param isCompact
     * @param separator
     * @param values
     * @return
     */
    public static String join(boolean isCompact, String separator, Object[] values) {
        return join(isCompact, separator, values, 0);
    }

    /**
     * 追加
     *
     * @param isCompact
     * @param separator
     * @param seal
     * @param values
     * @return
     */
    public static String join(boolean isCompact, String separator, String seal, Object[] values) {
        return join(isCompact, separator, seal, values, 0);
    }

    /**
     * 追加
     *
     * @param isCompact
     * @param separator
     * @param beginSeal
     * @param endSeal
     * @param values
     * @return
     */
    public static String join(boolean isCompact, String separator, String beginSeal, String endSeal, Object[] values) {
        return join(isCompact, separator, beginSeal, endSeal, values, 0, values.length);
    }

    /**
     * 追加
     *
     * @param isCompact
     * @param values
     * @param beginIndex
     * @return
     */
    public static String join(boolean isCompact, Object[] values, int beginIndex) {
        return join(isCompact, "", values, beginIndex);
    }

    /**
     * 追加
     *
     * @param isCompact
     * @param separator
     * @param values
     * @param beginIndex
     * @return
     */
    public static String join(boolean isCompact, String separator, Object[] values, int beginIndex) {
        return join(isCompact, separator, "", values, beginIndex);
    }

    /**
     * 追加
     *
     * @param isCompact
     * @param separator
     * @param seal
     * @param values
     * @param beginIndex
     * @return
     */
    public static String join(boolean isCompact, String separator, String seal, Object[] values, int beginIndex) {
        return join(isCompact, separator, seal, seal, values, beginIndex, values == null ? -1 : values.length);
    }

    /**
     * 追加
     *
     * @param separator
     * @param beginSeal
     * @param endSeal
     * @param values
     * @param beginIndex
     * @param count
     * @param callable
     * @return
     */
    public static String join(boolean isCompact, String separator, String beginSeal, String endSeal, Object[] values, int beginIndex, int count) {
        separator = Helpers.coalesce(separator, "");
        beginSeal = Helpers.coalesce(beginSeal, "");
        endSeal = Helpers.coalesce(endSeal, "");
        beginIndex = Math.max(beginIndex, 0);
        count = Math.min(beginIndex + count, values == null ? -1 : values.length);
        if (beginIndex < count) {
            StringBuffer buffer = new StringBuffer(count-- * 32);
            int amount = beginIndex - 1;
            while (++amount < count) {
                if (Strings.isEmpty(values[amount])) {
                    if (isCompact) {
                        continue;
                    }
                    buffer.append("null");
                } else {
                    buffer.append(beginSeal).append(values[amount]).append(endSeal);
                }
                buffer.append(separator);
            }
            if (values[amount] == null) {
                if (isCompact) {
                    if (buffer.length() > 0) {
                        buffer.setLength(buffer.length() - separator.length());
                    }
                } else {
                    buffer.append("null");
                }
            } else {
                buffer.append(beginSeal).append(values[amount]).append(endSeal);
            }
            return buffer.toString();
        }
        return null;
    }

    /**
     * 
     *
     * @param values
     * @return
     */
    public static String[] trim(String[] values) {
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                values[i] = Strings.empty(values[i]);
            }
        }
        return values;
    }

}

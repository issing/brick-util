package net.isger.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
     * 空字符串
     * 
     * @param value
     * @return
     */
    public static boolean isEmpty(Object value) {
        return value == null || value.toString().matches("^\\s*$");
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
        return isEmpty(value) ? def : value.toString().trim();
    }

    /**
     * 忽略大小写匹配
     * 
     * @param value
     * @param regex
     * @return
     */
    public static boolean matchsIgnoreCase(String value, String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(value)
                .matches();
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
    public static String replaceIgnoreCase(String value, String regex,
            String content) {
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
    public static String toCharset(byte[] data, String sourceCharset,
            String targetCharset) throws UnsupportedEncodingException {
        if (isEmpty(sourceCharset) || sourceCharset.equals(targetCharset)) {
            return new String(data, targetCharset);
        }
        return new String(
                Charset.forName(targetCharset)
                        .encode(Charset.forName(sourceCharset)
                                .decode(ByteBuffer.wrap(data)))
                        .array(),
                targetCharset);
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
    public static Object[] each(String value, String token,
            Callable<Object> callable) {
        return each(new StringTokenizer(value, token), callable);
    }

    /**
     * 遍历操作
     * 
     * @param tokenizer
     * @param callable
     * @return
     */
    public static Object[] each(StringTokenizer tokenizer,
            Callable<Object> callable) {
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
    public static String append(String[] values) {
        return append("", "", values, 0, values.length);
    }

    /**
     * 追加
     * 
     * @param values
     * @param beginIndex
     * @return
     */
    public static String append(String[] values, int beginIndex) {
        return append("", "", values, beginIndex, values.length);
    }

    /**
     * 追加
     * 
     * @param values
     * @param beginIndex
     * @param count
     * @return
     */
    public static String append(String[] values, int beginIndex, int count) {
        return append("", "", values, beginIndex, count);
    }

    /**
     * 追加
     * 
     * @param separator
     * @param values
     * @return
     */
    public static String append(String separator, String[] values) {
        return append(separator, "", values, 0, values.length);
    }

    /**
     * 追加
     * 
     * @param separator
     * @param values
     * @param beginIndex
     * @return
     */
    public static String append(String separator, String[] values,
            int beginIndex) {
        return append(separator, "", values, beginIndex, values.length);
    }

    /**
     * 追加
     * 
     * @param separator
     * @param values
     * @param beginIndex
     * @param count
     * @return
     */
    public static String append(String separator, String[] values,
            int beginIndex, int count) {
        return append(separator, "", values, beginIndex, count);
    }

    /**
     * 追加
     * 
     * @param separator
     * @param seal
     * @param values
     * @param beginIndex
     * @return
     */
    public static String append(String separator, String seal, String[] values,
            int beginIndex) {
        return append(separator, seal, values, beginIndex, values.length);
    }

    /**
     * 追加
     * 
     * @param separator
     * @param seal
     * @param values
     * @param beginIndex
     * @param count
     * @return
     */
    public static String append(String separator, String seal, String[] values,
            int beginIndex, int count) {
        separator = empty(separator);
        beginIndex = Math.max(beginIndex, 0);
        int size = Math.min(beginIndex + count, values.length);
        String result = null;
        if (beginIndex < size) {
            String[] pair = empty(seal).split("[|]", 2);
            String beginSeal = pair[0];
            String endSeal = pair.length == 1 ? beginSeal : pair[1];
            if (size > beginIndex) {
                StringBuffer buffer = new StringBuffer(size-- * 32);
                int i = beginIndex - 1;
                while (++i < size) {
                    buffer.append(beginSeal).append(values[i]).append(endSeal)
                            .append(separator);
                }
                buffer.append(beginSeal).append(values[i]).append(endSeal);
                result = buffer.toString();
            }
        }
        return result;
    }

}

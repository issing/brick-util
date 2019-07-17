package net.isger.util;

/**
 * 占位工具
 * 
 * @author issing
 */
public class Placeholders {

    private Placeholders() {
    }

    public static Object parse(String expression, Callable<Object> displacer) {
        if (Strings.isEmpty(expression)) {
            return expression;
        }
        char[] chars = expression.toCharArray();
        int size = chars.length;
        Placeholder placeholder = new Placeholder(size * 2, displacer);
        int index = parse(placeholder, chars, 0, size, false);
        if (index > 0 && placeholder.buffer.length() == 0) {
            return placeholder.value;
        }
        return placeholder.buffer.toString();
    }

    private static int parse(Placeholder holder, char[] chars, int index, int size, boolean holded) {
        /* 占位解析 */
        char c;
        parse: for (boolean holding = false; index < size; index++) {
            if ((c = chars[index]) == '$' && !holding) {
                holding = true; // 占位准备状态
                continue;
            } else if (holding) {
                holding = false; // 占位空闲状态
                hold: {
                    switch (c) {
                    case '{':
                        index = parsePlaceholder(holder, chars, index + 1, size);
                        if (index < 0) {
                            break parse; // 无效占位
                        }
                        continue;
                    case '$':
                        break hold; // 占位转义
                    }
                    holder.buffer.append('$');
                }
            } else if (holded && c == '}') {
                break; // 完成占位解析
            }
            holder.buffer.append(c);
        }
        return index;
    }

    private static int parsePlaceholder(Placeholder holder, char[] chars, int index, int size) {
        Placeholder embedHolder = new Placeholder(size, holder.callable);
        index = parse(embedHolder, chars, index, size, true); // 深度解析
        if (index == size && chars[index - 1] != '}') {
            holder.buffer.append("${"); // 追加无效占位符号
            holder.buffer.append(embedHolder.buffer);
            holder.value = holder.buffer.toString();
            index = -1;
        } else if (index > 0 && embedHolder.hold()) {
            holder.value = parse((String) embedHolder.value, embedHolder.callable); // 占位值
        } else {
            holder.buffer.append(embedHolder.value);
        }
        return index;
    }

    private static class Placeholder {

        private StringBuffer buffer;

        private Callable<Object> callable;

        private Object value;

        private boolean isHold;

        public Placeholder(int size, Callable<Object> callable) {
            this.buffer = new StringBuffer(size < 64 ? 64 : size);
            this.callable = callable;
        }

        public boolean hold() {
            if (value == null && buffer.length() > 0) {
                String key = buffer.toString();
                value = callable.call(Strings.trim(key));
                if (!(isHold = value != null)) {
                    value = "${" + key + "}";
                }
            } else if (value instanceof Placeholder) {
                value = ((Placeholder) value).value;
                if (buffer.length() > 0) {
                    value = buffer.append(value).toString();
                }
            }
            return isHold && value instanceof String;
        }
    }

}

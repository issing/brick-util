package net.isger.util;

import java.util.HashMap;
import java.util.Map;

import net.isger.util.anno.Ignore;
import net.isger.util.anno.Ignore.Mode;

public class BaseExtendable implements Extendable {

    /** 扩展值（默认不做序列化，但受传递注解影响） */
    @Ignore(mode = Mode.EXCLUDE, serialize = false)
    private Map<String, Object> values;

    public BaseExtendable() {
        this.values = new HashMap<String, Object>();
    }

    public Object getExtend(String name) {
        return values.get(name);
    }

    public void setExtend(String name, Object value) {
        values.put(name, value);
    }

    public Map<String, Object> getExtends() {
        return values;
    }

    public void setExtends(Map<String, ? extends Object> values) {
        this.clearExtends();
        this.values.putAll(values);
    }

    public void clearExtends() {
        this.values.clear();
    }

    public String toString() {
        return Helpers.toJson(this);
    }

}

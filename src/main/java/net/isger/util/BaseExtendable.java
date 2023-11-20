package net.isger.util;

import java.util.HashMap;
import java.util.Map;

import net.isger.util.anno.Ignore;
import net.isger.util.anno.Ignore.Mode;

public class BaseExtendable implements Extendable {

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

    public void setExtends(Map<String, Object> values) {
        this.values.clear();
        this.values.putAll(values);
    }

    public String toString() {
        return Helpers.toJson(this);
    }

}

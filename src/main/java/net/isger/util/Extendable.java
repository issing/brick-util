package net.isger.util;

import java.util.Map;

public interface Extendable {

    public Object getExtend(String name);

    public void setExtend(String name, Object value);

    public Map<String, Object> getExtends();

    public void setExtends(Map<String, Object> values);

}

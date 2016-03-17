package net.isger.util.reflect;

public interface Construction {

    public boolean isSupport(Class<?> clazz);

    public <T> T construct(Class<? extends T> clazz, Object... args);

}

package net.isger.util.reflect;

public interface Construction {

    public boolean isSupport(Class<?> rawClass);

    public <T> T construct(Class<? extends T> rawClass, Object... args);

}

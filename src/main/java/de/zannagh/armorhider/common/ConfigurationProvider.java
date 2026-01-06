package de.zannagh.armorhider.common;

public interface ConfigurationProvider<T> {
    T load();
    void save(T currentValue);
    T getDefault();
}

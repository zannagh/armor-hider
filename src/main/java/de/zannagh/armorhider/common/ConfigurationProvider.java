package de.zannagh.armorhider.common;

public interface ConfigurationProvider<T> {
    T load();
    void save(T currentValue);
    void saveCurrent();
    void setValue(T newValue);
    T getValue();
    T getDefault();
}

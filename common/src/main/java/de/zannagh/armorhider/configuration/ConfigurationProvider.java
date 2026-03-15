package de.zannagh.armorhider.configuration;

public interface ConfigurationProvider<T> {
    T load();

    void save(T currentValue);

    void saveCurrent();

    T getValue();

    void setValue(T newValue);

    T getDefault();
}

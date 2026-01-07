package de.zannagh.armorhider.configuration;

public abstract class ConfigurationItemBase<T> {

    protected T value;

    public ConfigurationItemBase(T actualValue) {
        this.value = actualValue;
    }
    
    public ConfigurationItemBase() {
        this.value = getDefaultValue();
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        if (value == null) {
            this.value = getDefaultValue();
            return;
        }
        this.value = value;
    }

    public abstract T getDefaultValue();
}

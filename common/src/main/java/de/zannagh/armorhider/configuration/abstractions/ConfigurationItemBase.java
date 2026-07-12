package de.zannagh.armorhider.configuration.abstractions;

/**
 * Represents a base class for configuration items, encapsulating a single value of a generic type.
 * This class provides mechanisms for managing the value, defaulting it where necessary,
 * and enforcing type safety.
 *
 * @param <T> The type of the value held by this configuration item.
 */
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

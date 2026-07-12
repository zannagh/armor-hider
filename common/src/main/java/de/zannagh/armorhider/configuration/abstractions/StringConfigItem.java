package de.zannagh.armorhider.configuration.abstractions;

/**
 * A string-based configuration item extending {@link ConfigurationItemBase} with the type parameter {@code String}.
 * This abstract class provides a framework for managing and persisting string configuration values
 * within an application.<br/><br/>
 *
 * Subclasses are required to implement the {@code getDefaultValue} method to specify a default
 * string value used when no custom value is provided or when the stored value is null.<br/><br/>
 *
 * Constructors:
 * - {@link StringConfigItem#StringConfigItem(String)}: Initializes the configuration item with
 *   a specific string value.
 * - {@link StringConfigItem#StringConfigItem()}: Initializes the configuration item with a default
 *   string value as defined by the {@code getDefaultValue} method.
 */
public abstract class StringConfigItem extends ConfigurationItemBase<String> {

    public StringConfigItem(String currentValue) {
        super(currentValue);
    }

    public StringConfigItem() {
        super();
    }
}

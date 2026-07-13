package de.zannagh.armorhider.configuration.abstractions;

/**
 * A configuration item specifically designed for handling double-precision numeric values.
 * This class extends the {@link ConfigurationItemBase} with the type parameter {@code Double},
 * allowing it to store, retrieve, and manage double values with type safety.<br/><br/>
 *
 * This is an abstract class that requires the implementation of the {@code getDefaultValue} method
 * in its subclasses to define a default value for the configuration item.<br/><br/>
 *
 * The primary use case of this class is to represent numeric configuration data
 * such as opacity levels, thresholds, or any other application-specific double values.<br/><br/>
 *
 * Constructors:
 * - {@link DoubleConfigurationItem#DoubleConfigurationItem(Double)}: Initializes the configuration item
 *   with a specific value.
 * - {@link DoubleConfigurationItem#DoubleConfigurationItem()}: Initializes the configuration item
 *   with a default value, as defined by the {@code getDefaultValue} method.<br/><br/>
 *
 * Subclasses should implement the {@code getDefaultValue} method to return a meaningful default
 * value for the specific use case of the configuration item.
 */
public abstract class DoubleConfigurationItem extends ConfigurationItemBase<Double> {

    public DoubleConfigurationItem(Double currentValue) {
        super(currentValue);
    }

    public DoubleConfigurationItem() {
        super();
    }
}

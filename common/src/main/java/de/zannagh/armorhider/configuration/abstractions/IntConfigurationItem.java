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
 * - {@link IntConfigurationItem#IntConfigurationItem(Integer)}: Initializes the configuration item
 *   with a specific value.
 * - {@link IntConfigurationItem#IntConfigurationItem()}: Initializes the configuration item
 *   with a default value, as defined by the {@code getDefaultValue} method.<br/><br/>
 *
 * Subclasses should implement the {@code getDefaultValue} method to return a meaningful default
 * value for the specific use case of the configuration item.
 */
public abstract class IntConfigurationItem extends ConfigurationItemBase<Integer> {

    public IntConfigurationItem(Integer currentValue) {
        super(currentValue);
    }

    public IntConfigurationItem() {
        super();
    }

    /** Lower bound, inclusive. Unbounded by default; range-limited items override it. */
    protected Integer getMinValue() {
        return Integer.MIN_VALUE;
    }

    /** Upper bound, inclusive. Unbounded by default; range-limited items override it. */
    protected Integer getMaxValue() {
        return Integer.MAX_VALUE;
    }

    /**
     * Rejects non-finite values (NaN / ±Infinity) outright and clamps everything else into
     * {@code [getMinValue(), getMaxValue()]}. NaN in particular has to be caught here: it round-trips
     * through the config item happily but makes {@code Gson#toJson} throw {@link IllegalArgumentException},
     * which escapes the IOException-only catch in the save path and can leave a settings screen unclosable.
     */
    @Override
    protected Integer sanitize(Integer candidate) {
        if (candidate == null || !Double.isFinite(candidate)) {
            return getDefaultValue();
        }
        // Math.clamp is Java 21+; 1.20.1 builds on Java 17.
        return Math.min(Math.max(candidate, getMinValue()), getMaxValue());
    }
}

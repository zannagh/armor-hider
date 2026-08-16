package de.zannagh.armorhider.configuration.abstractions;

/**
 * A configuration item specifically designed for handling integer numeric values.
 * This class extends {@link ConfigurationItemBase} with the type parameter {@code Integer},
 * allowing it to store, retrieve, and manage integer values with type safety.
 *
 * Subclasses must implement {@link #getDefaultValue()} and may override {@link #getMinValue()} and
 * {@link #getMaxValue()} to constrain the allowed range.
 *
 * Values are sanitized via {@link #sanitize(Integer)} by clamping into the configured range and
 * falling back to the default when {@code null}.
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
     * Clamps {@code candidate} into {@code [getMinValue(), getMaxValue()]} and falls back to the default
     * when {@code null}.
     */
    @Override
    protected Integer sanitize(Integer candidate) {
        if (candidate == null) {
            return getDefaultValue();
        }
        // Math.clamp is Java 21+; 1.20.1 builds on Java 17.
        return Math.min(Math.max(candidate, getMinValue()), getMaxValue());
    }
}

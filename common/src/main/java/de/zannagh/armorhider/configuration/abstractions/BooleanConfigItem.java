package de.zannagh.armorhider.configuration.abstractions;

/**
 * A boolean configuration item, where {@link BooleanConfigItem getValue} will return a boolean value.<br/>
 *
 * Also see {@link ConfigurationItemBase}.
 */
public abstract class BooleanConfigItem extends ConfigurationItemBase<Boolean> {

    public BooleanConfigItem(Boolean currentValue) {
        super(currentValue);
    }

    public BooleanConfigItem() {
        super();
    }
}

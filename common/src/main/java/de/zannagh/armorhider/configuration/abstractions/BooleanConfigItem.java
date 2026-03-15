package de.zannagh.armorhider.configuration.abstractions;

public abstract class BooleanConfigItem extends ConfigurationItemBase<Boolean> {

    public BooleanConfigItem(Boolean currentValue) {
        super(currentValue);
    }

    public BooleanConfigItem() {
        super();
    }
}

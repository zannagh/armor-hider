package de.zannagh.armorhider.configuration.abstractions;

public abstract class DoubleConfigurationItem extends ConfigurationItemBase<Double> {

    public DoubleConfigurationItem(Double currentValue) {
        super(currentValue);
    }

    public DoubleConfigurationItem() {
        super();
    }
}

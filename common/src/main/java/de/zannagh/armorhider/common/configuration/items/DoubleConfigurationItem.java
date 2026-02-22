package de.zannagh.armorhider.common.configuration.items;

import de.zannagh.armorhider.common.configuration.ConfigurationItemBase;

public abstract class DoubleConfigurationItem extends ConfigurationItemBase<Double> {

    public DoubleConfigurationItem(Double currentValue) {
        super(currentValue);
    }

    public DoubleConfigurationItem() {
        super();
    }
}

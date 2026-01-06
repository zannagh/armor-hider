package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.ConfigurationItemBase;

public abstract class DoubleConfigurationItem extends ConfigurationItemBase<Double> {

    public DoubleConfigurationItem(double currentValue) {
        super(currentValue);
    }

    public DoubleConfigurationItem(){
        super();
    }
}

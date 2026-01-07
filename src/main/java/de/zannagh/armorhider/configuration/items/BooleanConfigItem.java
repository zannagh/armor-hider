package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.ConfigurationItemBase;

public abstract class BooleanConfigItem extends ConfigurationItemBase<Boolean> {

    public BooleanConfigItem(Boolean currentValue) {
        super(currentValue);
    }

    public BooleanConfigItem() {
        super();
    }
}

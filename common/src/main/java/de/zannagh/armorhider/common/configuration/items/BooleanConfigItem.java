package de.zannagh.armorhider.common.configuration.items;

import de.zannagh.armorhider.common.configuration.ConfigurationItemBase;

public abstract class BooleanConfigItem extends ConfigurationItemBase<Boolean> {

    public BooleanConfigItem(Boolean currentValue) {
        super(currentValue);
    }

    public BooleanConfigItem() {
        super();
    }
}

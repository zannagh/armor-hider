package de.zannagh.armorhider.common.configuration.items;

import de.zannagh.armorhider.common.configuration.ConfigurationItemBase;

public abstract class StringConfigItem extends ConfigurationItemBase<String> {

    public StringConfigItem(String currentValue) {
        super(currentValue);
    }

    public StringConfigItem() {
        super();
    }
}

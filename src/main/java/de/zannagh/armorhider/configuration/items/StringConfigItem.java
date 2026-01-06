package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.ConfigurationItemBase;

public abstract class StringConfigItem extends ConfigurationItemBase<String> {

    public StringConfigItem(String currentValue) {
        super(currentValue);
    }
    
    public StringConfigItem(){
        super();
    }
}

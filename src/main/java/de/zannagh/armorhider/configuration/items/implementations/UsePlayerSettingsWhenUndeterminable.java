package de.zannagh.armorhider.configuration.items.implementations;

import de.zannagh.armorhider.configuration.items.BooleanConfigItem;

public class UsePlayerSettingsWhenUndeterminable extends BooleanConfigItem {

    public UsePlayerSettingsWhenUndeterminable(boolean currentValue) {
        super(currentValue);
    }

    public UsePlayerSettingsWhenUndeterminable() {
        super();
    }
    
    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}

package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

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

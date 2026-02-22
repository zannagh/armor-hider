package de.zannagh.armorhider.common.configuration.items.implementations;

import de.zannagh.armorhider.common.configuration.items.BooleanConfigItem;

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

package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

public class DisableArmorHiderOnInvisibility extends BooleanConfigItem {

    public DisableArmorHiderOnInvisibility(boolean currentValue) {
        super(currentValue);
    }

    public DisableArmorHiderOnInvisibility() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}

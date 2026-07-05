package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

public class DisableArmorHiderOnInvisibilityGlobally extends BooleanConfigItem {

    public DisableArmorHiderOnInvisibilityGlobally(boolean currentValue) {
        super(currentValue);
    }

    public DisableArmorHiderOnInvisibilityGlobally() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}

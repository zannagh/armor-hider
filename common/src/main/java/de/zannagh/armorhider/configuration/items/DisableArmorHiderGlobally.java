package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

public class DisableArmorHiderGlobally extends BooleanConfigItem {
    public DisableArmorHiderGlobally(boolean currentValue) {
        super(currentValue);
    }

    public DisableArmorHiderGlobally() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}

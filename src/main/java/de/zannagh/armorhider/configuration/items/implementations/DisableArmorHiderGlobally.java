package de.zannagh.armorhider.configuration.items.implementations;

import de.zannagh.armorhider.configuration.items.BooleanConfigItem;

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

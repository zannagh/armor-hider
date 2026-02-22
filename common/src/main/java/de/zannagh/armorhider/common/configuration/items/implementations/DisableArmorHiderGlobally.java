package de.zannagh.armorhider.common.configuration.items.implementations;

import de.zannagh.armorhider.common.configuration.items.BooleanConfigItem;

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

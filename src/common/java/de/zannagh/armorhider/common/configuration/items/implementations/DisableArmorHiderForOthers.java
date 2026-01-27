package de.zannagh.armorhider.common.configuration.items.implementations;

import de.zannagh.armorhider.common.configuration.items.BooleanConfigItem;

public class DisableArmorHiderForOthers extends BooleanConfigItem {

    public DisableArmorHiderForOthers(boolean currentValue) {
        super(currentValue);
    }

    public DisableArmorHiderForOthers() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}

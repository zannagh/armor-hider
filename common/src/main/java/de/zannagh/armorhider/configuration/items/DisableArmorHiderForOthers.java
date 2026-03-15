package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

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

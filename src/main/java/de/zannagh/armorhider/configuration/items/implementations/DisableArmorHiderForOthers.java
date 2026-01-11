package de.zannagh.armorhider.configuration.items.implementations;

import de.zannagh.armorhider.configuration.items.BooleanConfigItem;

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

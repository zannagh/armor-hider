package de.zannagh.armorhider.configuration.items.implementations;

import de.zannagh.armorhider.configuration.items.BooleanConfigItem;

public class OpacityAffectingHatOrSkullItem extends BooleanConfigItem {
    public OpacityAffectingHatOrSkullItem(boolean currentValue) {
        super(currentValue);
    }

    public OpacityAffectingHatOrSkullItem() {
        super();
    }
    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}

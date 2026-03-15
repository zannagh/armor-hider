package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

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

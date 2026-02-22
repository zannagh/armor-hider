package de.zannagh.armorhider.common.configuration.items.implementations;

import de.zannagh.armorhider.common.configuration.items.BooleanConfigItem;

public class OpacityAffectingElytraItem extends BooleanConfigItem {
    public OpacityAffectingElytraItem(boolean currentValue) {
        super(currentValue);
    }

    public OpacityAffectingElytraItem() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}

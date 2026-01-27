package de.zannagh.armorhider.common.configuration.items.implementations;

import de.zannagh.armorhider.common.configuration.items.BooleanConfigItem;

public class ForceArmorHiderOffOnPlayers extends BooleanConfigItem {

    public ForceArmorHiderOffOnPlayers(boolean currentValue) {
        super(currentValue);
    }

    public ForceArmorHiderOffOnPlayers() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}

package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

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

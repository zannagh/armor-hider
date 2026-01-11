package de.zannagh.armorhider.configuration.items.implementations;

import de.zannagh.armorhider.configuration.items.BooleanConfigItem;

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

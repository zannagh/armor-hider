package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

public class InCombatUseDefaultArmorSkin extends BooleanConfigItem {

    public InCombatUseDefaultArmorSkin(boolean currentValue) {
        super(currentValue);
    }

    public InCombatUseDefaultArmorSkin() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}

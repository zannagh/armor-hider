package de.zannagh.armorhider.configuration.items.implementations;

import de.zannagh.armorhider.configuration.items.BooleanConfigItem;

public class ToggleArmorHiderForOthersItem extends BooleanConfigItem {

    public ToggleArmorHiderForOthersItem(boolean currentValue) {
        super(currentValue);
    }

    public ToggleArmorHiderForOthersItem() {
        super();
    }
   
    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}

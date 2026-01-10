package de.zannagh.armorhider.configuration.items.implementations;

import de.zannagh.armorhider.configuration.items.BooleanConfigItem;

public class GlobalArmorHiderToggleItem extends BooleanConfigItem {
    public GlobalArmorHiderToggleItem(boolean currentValue) {
        super(currentValue);
    }

    public GlobalArmorHiderToggleItem() {
        super();
    }
    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}

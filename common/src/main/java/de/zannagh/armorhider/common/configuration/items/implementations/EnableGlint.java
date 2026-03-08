package de.zannagh.armorhider.common.configuration.items.implementations;

import de.zannagh.armorhider.common.configuration.items.BooleanConfigItem;

public class EnableGlint extends BooleanConfigItem {
    public EnableGlint(boolean currentValue) {
        super(currentValue);
    }

    public EnableGlint() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}

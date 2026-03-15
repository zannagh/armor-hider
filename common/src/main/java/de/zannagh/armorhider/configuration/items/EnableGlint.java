package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

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

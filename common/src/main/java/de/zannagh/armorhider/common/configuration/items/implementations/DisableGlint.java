package de.zannagh.armorhider.common.configuration.items.implementations;

import de.zannagh.armorhider.common.configuration.items.BooleanConfigItem;

public class DisableGlint extends BooleanConfigItem {
    public DisableGlint(boolean currentValue) {
        super(currentValue);
    }

    public DisableGlint() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
